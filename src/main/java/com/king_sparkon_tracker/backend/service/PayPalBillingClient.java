package com.king_sparkon_tracker.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.king_sparkon_tracker.backend.config.PayPalProperties;
import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PayPalBillingClient {

	private final PayPalProperties properties;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	public PayPalBillingClient(PayPalProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public CreatedPayPalSubscription createSubscription(
			BusinessPlan businessPlan,
			BillingInterval billingInterval,
			Integer termYears) {
		String accessToken = accessToken();
		String planId = properties.planId(businessPlan, billingInterval);

		Map<String, Object> payload = Map.of(
				"plan_id", planId,
				"custom_id", businessPlan.name() + "-" + billingInterval.name() + "-" + (termYears == null ? 1 : termYears),
				"application_context", Map.of(
						"brand_name", "King Sparkon Tracker",
						"locale", "en-ZA",
						"shipping_preference", "NO_SHIPPING",
						"user_action", "SUBSCRIBE_NOW",
						"return_url", properties.getReturnUrl(),
						"cancel_url", properties.getCancelUrl()
				)
		);

		Map<?, ?> response = restTemplate.postForObject(
				properties.getBaseUrl() + "/v1/billing/subscriptions",
				new HttpEntity<>(payload, bearerJsonHeaders(accessToken)),
				Map.class
		);

		if (response == null || response.get("id") == null) {
			throw new IllegalStateException("PayPal subscription creation failed");
		}

		String subscriptionId = String.valueOf(response.get("id"));
		String status = response.get("status") == null
				? "APPROVAL_PENDING"
				: String.valueOf(response.get("status"));
		String approvalUrl = approvalUrl(response);

		return new CreatedPayPalSubscription(subscriptionId, subscriptionId, planId, status, approvalUrl);
	}

	public String subscriptionStatus(String paypalSubscriptionId) {
		String accessToken = accessToken();

		Map<?, ?> response = restTemplate.getForObject(
				properties.getBaseUrl() + "/v1/billing/subscriptions/" + paypalSubscriptionId,
				Map.class,
				bearerJsonHeaders(accessToken)
		);

		return response == null ? null : String.valueOf(response.get("status"));
	}

	public boolean verifyWebhookSignature(
			String rawPayload,
			String transmissionId,
			String transmissionTime,
			String certUrl,
			String authAlgo,
			String transmissionSignature) {
		String accessToken = accessToken();

		try {
			Map<String, Object> webhookEvent = objectMapper.readValue(rawPayload, new TypeReference<>() {
			});

			Map<String, Object> payload = Map.of(
					"transmission_id", transmissionId,
					"transmission_time", transmissionTime,
					"cert_url", certUrl,
					"auth_algo", authAlgo,
					"transmission_sig", transmissionSignature,
					"webhook_id", properties.getWebhookId(),
					"webhook_event", webhookEvent
			);

			Map<?, ?> response = restTemplate.postForObject(
					properties.getBaseUrl() + "/v1/notifications/verify-webhook-signature",
					new HttpEntity<>(payload, bearerJsonHeaders(accessToken)),
					Map.class
			);

			return response != null && "SUCCESS".equalsIgnoreCase(String.valueOf(response.get("verification_status")));
		} catch (Exception exception) {
			return false;
		}
	}

	private String accessToken() {
		String credentials = properties.getClientId() + ":" + properties.getClientSecret();
		String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);

		Map<?, ?> response = restTemplate.postForObject(
				properties.getBaseUrl() + "/v1/oauth2/token",
				new HttpEntity<>("grant_type=client_credentials", headers),
				Map.class
		);

		if (response == null || response.get("access_token") == null) {
			throw new IllegalStateException("Unable to fetch PayPal access token");
		}

		return String.valueOf(response.get("access_token"));
	}

	private HttpHeaders bearerJsonHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		return headers;
	}

	private String approvalUrl(Map<?, ?> response) {
		Object linksObject = response.get("links");

		if (!(linksObject instanceof List<?> links)) {
			return null;
		}

		for (Object linkObject : links) {
			if (linkObject instanceof Map<?, ?> link && "approve".equals(String.valueOf(link.get("rel")))) {
				return String.valueOf(link.get("href"));
			}
		}

		return null;
	}

	public record CreatedPayPalSubscription(
			String paypalSubscriptionId,
			String paypalSubscriptionToken,
			String paypalPlanId,
			String status,
			String approvalUrl
	) {
	}
}
