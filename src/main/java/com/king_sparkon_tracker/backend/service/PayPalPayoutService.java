package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class PayPalPayoutService {

	private static final int MONEY_SCALE = 2;

	private final RestClient restClient;
	private final boolean enabled;
	private final String clientId;
	private final String clientSecret;
	private final String payoutCurrency;
	private final BigDecimal zarPerPayoutUnit;

	public PayPalPayoutService(
			@Value("${paypal.base-url:https://api-m.sandbox.paypal.com}") String baseUrl,
			@Value("${paypal.payouts.enabled:${paypal.enabled:false}}") boolean enabled,
			@Value("${paypal.client-id:}") String clientId,
			@Value("${paypal.client-secret:}") String clientSecret,
			@Value("${paypal.payouts.currency:USD}") String payoutCurrency,
			@Value("${paypal.payouts.zar-per-unit:0}") BigDecimal zarPerPayoutUnit) {
		this.restClient = RestClient.builder().baseUrl(required(baseUrl, "PayPal base URL is required")).build();
		this.enabled = enabled;
		this.clientId = clientId == null ? "" : clientId.trim();
		this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
		this.payoutCurrency = required(payoutCurrency, "PayPal payout currency is required").toUpperCase(Locale.ROOT);
		this.zarPerPayoutUnit = money(zarPerPayoutUnit);
	}

	public boolean isConfigured() {
		return enabled
				&& StringUtils.hasText(clientId)
				&& StringUtils.hasText(clientSecret)
				&& !"ZAR".equals(payoutCurrency)
				&& zarPerPayoutUnit.signum() > 0;
	}

	public String payoutCurrency() {
		return payoutCurrency;
	}

	public BigDecimal zarPerPayoutUnit() {
		return zarPerPayoutUnit;
	}

	public PayoutQuote quote(BigDecimal amountZar) {
		requireConfigured();
		BigDecimal normalizedZar = positiveMoney(amountZar, "PayPal payout amount must be greater than zero");
		BigDecimal payoutAmount = normalizedZar.divide(zarPerPayoutUnit, MONEY_SCALE, RoundingMode.HALF_UP);
		if (payoutAmount.signum() <= 0) {
			throw new IllegalArgumentException("PayPal converted payout amount must be greater than zero");
		}
		return new PayoutQuote(normalizedZar, payoutAmount, payoutCurrency, zarPerPayoutUnit);
	}

	public PayoutSubmission submitWithdrawal(Long withdrawalId, BigDecimal amountZar, String recipientEmail) {
		if (withdrawalId == null) {
			throw new IllegalArgumentException("Withdrawal id is required for PayPal idempotency");
		}
		String receiver = required(recipientEmail, "A PayPal email address is required").toLowerCase(Locale.ROOT);
		PayoutQuote quote = quote(amountZar);
		String requestId = "KST-OWNER-WITHDRAWAL-" + withdrawalId;

		PayoutCreateRequest request = new PayoutCreateRequest(
				new SenderBatchHeader(
						requestId,
						"Your King Sparkon withdrawal is processing",
						"King Sparkon sent your business balance withdrawal to PayPal."),
				List.of(new PayoutItem(
						"EMAIL",
						receiver,
						new PayoutAmount(quote.payoutAmount().toPlainString(), quote.payoutCurrency()),
						"King Sparkon owner balance withdrawal",
						requestId,
						"PAYPAL")));

		try {
			PayoutResponse response = restClient.post()
					.uri("/v1/payments/payouts")
					.headers(headers -> headers.setBearerAuth(accessToken()))
					.header("PayPal-Request-Id", requestId)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(PayoutResponse.class);

			if (response == null || response.batchHeader() == null
					|| !StringUtils.hasText(response.batchHeader().payoutBatchId())) {
				throw new IllegalStateException("PayPal did not return a payout batch id");
			}
			return new PayoutSubmission(
					response.batchHeader().payoutBatchId(),
					normalizeStatus(response.batchHeader().batchStatus()),
					quote);
		} catch (RestClientResponseException exception) {
			throw paypalFailure("PayPal payout submission failed", exception);
		}
	}

	public String getBatchStatus(String payoutBatchId) {
		String batchId = required(payoutBatchId, "PayPal payout batch id is required");
		requireConfigured();
		try {
			PayoutResponse response = restClient.get()
					.uri("/v1/payments/payouts/{batchId}?fields=batch_header", batchId)
					.headers(headers -> headers.setBearerAuth(accessToken()))
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(PayoutResponse.class);
			if (response == null || response.batchHeader() == null) {
				throw new IllegalStateException("PayPal payout status response is empty");
			}
			return normalizeStatus(response.batchHeader().batchStatus());
		} catch (RestClientResponseException exception) {
			throw paypalFailure("PayPal payout status lookup failed", exception);
		}
	}

	private String accessToken() {
		requireConfigured();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");
		try {
			OAuthTokenResponse response = restClient.post()
					.uri("/v1/oauth2/token")
					.headers(headers -> headers.setBasicAuth(clientId, clientSecret))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.body(form)
					.retrieve()
					.body(OAuthTokenResponse.class);
			if (response == null || !StringUtils.hasText(response.accessToken())) {
				throw new IllegalStateException("PayPal OAuth token response is empty");
			}
			return response.accessToken();
		} catch (RestClientResponseException exception) {
			throw paypalFailure("PayPal authentication failed", exception);
		}
	}

	private void requireConfigured() {
		if (!enabled) {
			throw new IllegalStateException("PayPal payouts are disabled. Set PAYPAL_PAYOUTS_ENABLED=true after enabling Payouts on the PayPal business account");
		}
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
			throw new IllegalStateException("PayPal client credentials are not configured");
		}
		if ("ZAR".equals(payoutCurrency)) {
			throw new IllegalStateException("PayPal Payouts does not support ZAR. Configure PAYPAL_PAYOUTS_CURRENCY to a PayPal-supported currency");
		}
		if (zarPerPayoutUnit.signum() <= 0) {
			throw new IllegalStateException("PAYPAL_PAYOUTS_ZAR_PER_UNIT must be greater than zero");
		}
	}

	private IllegalStateException paypalFailure(String message, RestClientResponseException exception) {
		String details = exception.getResponseBodyAsString();
		return new IllegalStateException(
				message + " (HTTP " + exception.getStatusCode().value() + ")"
						+ (StringUtils.hasText(details) ? ": " + details : ""),
				exception);
	}

	private String normalizeStatus(String status) {
		return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "PENDING";
	}

	private BigDecimal positiveMoney(BigDecimal amount, String message) {
		BigDecimal normalized = money(amount);
		if (normalized.signum() <= 0) {
			throw new IllegalArgumentException(message);
		}
		return normalized;
	}

	private BigDecimal money(BigDecimal amount) {
		return amount == null
				? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private static String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	public record PayoutQuote(
			BigDecimal amountZar,
			BigDecimal payoutAmount,
			String payoutCurrency,
			BigDecimal zarPerPayoutUnit) {
	}

	public record PayoutSubmission(String payoutBatchId, String batchStatus, PayoutQuote quote) {
	}

	private record PayoutCreateRequest(
			@JsonProperty("sender_batch_header") SenderBatchHeader senderBatchHeader,
			List<PayoutItem> items) {
	}

	private record SenderBatchHeader(
			@JsonProperty("sender_batch_id") String senderBatchId,
			@JsonProperty("email_subject") String emailSubject,
			@JsonProperty("email_message") String emailMessage) {
	}

	private record PayoutItem(
			@JsonProperty("recipient_type") String recipientType,
			String receiver,
			PayoutAmount amount,
			String note,
			@JsonProperty("sender_item_id") String senderItemId,
			@JsonProperty("recipient_wallet") String recipientWallet) {
	}

	private record PayoutAmount(String value, String currency) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OAuthTokenResponse(@JsonProperty("access_token") String accessToken) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PayoutResponse(@JsonProperty("batch_header") PayoutBatchHeader batchHeader) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PayoutBatchHeader(
			@JsonProperty("payout_batch_id") String payoutBatchId,
			@JsonProperty("batch_status") String batchStatus) {
	}
}
