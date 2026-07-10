package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentLink;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.PaymentLinkCreateParams;

@Service
public class StripeService {

	private final String apiKey;

	public StripeService(@Value("${stripe.api-key:${stripe.secret-key:}}") String apiKey) {
		this.apiKey = apiKey;
	}

	public CreatedEmbeddedPaymentIntent createEmbeddedCartPaymentIntent(
			BigDecimal amount,
			String receiptEmail,
			Map<String, String> metadata,
			String idempotencyKey) {
		requireApiKey();
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Embedded cart payment amount must be greater than zero");
		}

		try {
			PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
					.setAmount(unitAmountInCents(amount))
					.setCurrency("zar")
					.putAllMetadata(metadata == null ? Map.of() : metadata)
					.setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
							.setEnabled(true)
							.build());
			if (receiptEmail != null && !receiptEmail.isBlank()) {
				builder.setReceiptEmail(receiptEmail.trim().toLowerCase());
			}

			RequestOptions.Builder requestOptions = RequestOptions.builder().setApiKey(apiKey);
			if (idempotencyKey != null && !idempotencyKey.isBlank()) {
				requestOptions.setIdempotencyKey(idempotencyKey.trim());
			}

			PaymentIntent intent = PaymentIntent.create(builder.build(), requestOptions.build());
			if (intent.getId() == null || intent.getClientSecret() == null) {
				throw new IllegalStateException("Stripe PaymentIntent creation failed");
			}
			return new CreatedEmbeddedPaymentIntent(intent.getId(), intent.getClientSecret(), intent.getStatus());
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe PaymentIntent creation failed", exception);
		}
	}

	public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
		requireApiKey();
		try {
			return PaymentIntent.retrieve(paymentIntentId, RequestOptions.builder().setApiKey(apiKey).build());
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe PaymentIntent lookup failed", exception);
		}
	}

	public PaymentIntent updatePaymentIntentMetadata(String paymentIntentId, Map<String, String> metadata) {
		requireApiKey();
		try {
			PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, RequestOptions.builder().setApiKey(apiKey).build());
			return intent.update(
					PaymentIntentUpdateParams.builder().putAllMetadata(metadata == null ? Map.of() : metadata).build(),
					RequestOptions.builder().setApiKey(apiKey).build());
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe PaymentIntent metadata update failed", exception);
		}
	}

	public CreatedTipPaymentLink createTipPaymentLink(
			Tip tip,
			BigDecimal systemFee,
			BigDecimal netAmount,
			String callbackUrl) {
		requireApiKey();

		try {
			PaymentLink paymentLink = PaymentLink.create(
					paymentLinkParams(tip, systemFee, netAmount, callbackUrl),
					RequestOptions.builder().setApiKey(apiKey).build());

			if (paymentLink.getId() == null || paymentLink.getUrl() == null) {
				throw new IllegalStateException("Stripe payment link creation failed");
			}

			return new CreatedTipPaymentLink(paymentLink.getId(), paymentLink.getUrl(), qrCodeUrl(paymentLink.getUrl()));
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe payment link creation failed", exception);
		}
	}

	public CreatedTransactionPaymentLink createTransactionPaymentLink(InventoryTransaction transaction) {
		requireApiKey();

		try {
			PaymentLink paymentLink = PaymentLink.create(
					transactionPaymentLinkParams(transaction),
					RequestOptions.builder().setApiKey(apiKey).build());

			if (paymentLink.getId() == null || paymentLink.getUrl() == null) {
				throw new IllegalStateException("Stripe transaction payment link creation failed");
			}

			return new CreatedTransactionPaymentLink(paymentLink.getId(), paymentLink.getUrl());
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe transaction payment link creation failed", exception);
		}
	}

	public CreatedBusinessTopUpPaymentLink createBusinessTopUpPaymentLink(Business business, BigDecimal amount, String callbackUrl, String paymentMethod) {
		requireApiKey();

		try {
			PaymentLink paymentLink = PaymentLink.create(
					businessTopUpPaymentLinkParams(business, amount, callbackUrl, paymentMethod),
					RequestOptions.builder().setApiKey(apiKey).build());

			if (paymentLink.getId() == null || paymentLink.getUrl() == null) {
				throw new IllegalStateException("Stripe business account top-up link creation failed");
			}

			return new CreatedBusinessTopUpPaymentLink(paymentLink.getId(), paymentLink.getUrl(), qrCodeUrl(paymentLink.getUrl()));
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe business account top-up link creation failed", exception);
		}
	}

	public CreatedTicketPaymentLink createTicketPaymentLink(TicketPayment payment, TicketEvent event, String callbackUrl) {
		requireApiKey();

		try {
			PaymentLink paymentLink = PaymentLink.create(
					ticketPaymentLinkParams(payment, event, callbackUrl),
					RequestOptions.builder().setApiKey(apiKey).build());

			if (paymentLink.getId() == null || paymentLink.getUrl() == null) {
				throw new IllegalStateException("Stripe ticket payment link creation failed");
			}

			return new CreatedTicketPaymentLink(paymentLink.getId(), paymentLink.getUrl(), qrCodeUrl(paymentLink.getUrl()));
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe ticket payment link creation failed", exception);
		}
	}

	private PaymentLinkCreateParams paymentLinkParams(Tip tip, BigDecimal systemFee, BigDecimal netAmount, String callbackUrl) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("tipId", stringValue(tip.getId()));
		metadata.put("workerId", stringValue(tip.getWorkerId()));
		metadata.put("systemFee", systemFee.toPlainString());
		metadata.put("netAmount", netAmount.toPlainString());

		PaymentLinkCreateParams.Builder builder = PaymentLinkCreateParams.builder()
				.putAllMetadata(metadata)
				.setPaymentIntentData(PaymentLinkCreateParams.PaymentIntentData.builder().putAllMetadata(metadata).build())
				.addLineItem(PaymentLinkCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(PaymentLinkCreateParams.LineItem.PriceData.builder()
								.setCurrency("zar")
								.setUnitAmount(unitAmountInCents(tip.getTipAmount()))
								.setProductData(PaymentLinkCreateParams.LineItem.PriceData.ProductData.builder().setName("King Sparkon worker tip").build())
								.build())
						.build());

		if (callbackUrl != null && !callbackUrl.isBlank()) {
			builder.setAfterCompletion(PaymentLinkCreateParams.AfterCompletion.builder()
					.setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
					.setRedirect(PaymentLinkCreateParams.AfterCompletion.Redirect.builder().setUrl(callbackUrl.trim()).build())
					.build());
		}

		return builder.build();
	}

	private PaymentLinkCreateParams transactionPaymentLinkParams(InventoryTransaction transaction) {
		BigDecimal totalAmount = transaction.getTotalAmount();
		if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transaction total must be greater than zero for website payment");
		}

		Map<String, String> metadata = new HashMap<>();
		metadata.put("transactionId", stringValue(transaction.getId()));
		metadata.put("businessId", stringValue(transaction.getBusiness() == null ? null : transaction.getBusiness().getId()));
		metadata.put("paymentEmail", stringValue(transaction.getPaymentEmail()));

		return PaymentLinkCreateParams.builder()
				.putAllMetadata(metadata)
				.setPaymentIntentData(PaymentLinkCreateParams.PaymentIntentData.builder().putAllMetadata(metadata).build())
				.addLineItem(PaymentLinkCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(PaymentLinkCreateParams.LineItem.PriceData.builder()
								.setCurrency("zar")
								.setUnitAmount(unitAmountInCents(totalAmount))
								.setProductData(PaymentLinkCreateParams.LineItem.PriceData.ProductData.builder()
										.setName("King Sparkon transaction #%s".formatted(transaction.getId()))
										.build())
								.build())
						.build())
				.build();
	}

	private PaymentLinkCreateParams businessTopUpPaymentLinkParams(Business business, BigDecimal amount, String callbackUrl, String paymentMethod) {
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Business account top-up amount must be greater than zero");
		}

		Map<String, String> metadata = new HashMap<>();
		metadata.put("businessId", stringValue(business.getId()));
		metadata.put("businessName", stringValue(business.getName()));
		metadata.put("paymentMethod", paymentMethod == null || paymentMethod.isBlank() ? "CARD_OR_WALLET" : paymentMethod.trim().toUpperCase());

		PaymentLinkCreateParams.Builder builder = PaymentLinkCreateParams.builder()
				.putAllMetadata(metadata)
				.setPaymentIntentData(PaymentLinkCreateParams.PaymentIntentData.builder().putAllMetadata(metadata).build())
				.addLineItem(PaymentLinkCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(PaymentLinkCreateParams.LineItem.PriceData.builder()
								.setCurrency("zar")
								.setUnitAmount(unitAmountInCents(amount))
								.setProductData(PaymentLinkCreateParams.LineItem.PriceData.ProductData.builder().setName("King Sparkon business account top-up").build())
								.build())
						.build());

		if (callbackUrl != null && !callbackUrl.isBlank()) {
			builder.setAfterCompletion(PaymentLinkCreateParams.AfterCompletion.builder()
					.setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
					.setRedirect(PaymentLinkCreateParams.AfterCompletion.Redirect.builder().setUrl(callbackUrl.trim()).build())
					.build());
		}
		return builder.build();
	}

	private PaymentLinkCreateParams ticketPaymentLinkParams(TicketPayment payment, TicketEvent event, String callbackUrl) {
		if (payment.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Ticket payment total must be greater than zero");
		}

		Map<String, String> metadata = new HashMap<>();
		metadata.put("ticketPaymentId", stringValue(payment.getId()));
		metadata.put("eventId", stringValue(payment.getEventId()));
		metadata.put("userId", stringValue(payment.getUserId()));
		metadata.put("ticketType", payment.getTicketType().name());
		metadata.put("quantity", stringValue(payment.getQuantity()));

		PaymentLinkCreateParams.Builder builder = PaymentLinkCreateParams.builder()
				.putAllMetadata(metadata)
				.setPaymentIntentData(PaymentLinkCreateParams.PaymentIntentData.builder().putAllMetadata(metadata).build())
				.addLineItem(PaymentLinkCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(PaymentLinkCreateParams.LineItem.PriceData.builder()
								.setCurrency("zar")
								.setUnitAmount(unitAmountInCents(payment.getTotalAmount()))
								.setProductData(PaymentLinkCreateParams.LineItem.PriceData.ProductData.builder()
										.setName("King Sparkon ticket checkout - " + event.getName())
										.build())
								.build())
						.build());

		if (callbackUrl != null && !callbackUrl.isBlank()) {
			builder.setAfterCompletion(PaymentLinkCreateParams.AfterCompletion.builder()
					.setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
					.setRedirect(PaymentLinkCreateParams.AfterCompletion.Redirect.builder().setUrl(callbackUrl.trim()).build())
					.build());
		}
		return builder.build();
	}

	private void requireApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("Stripe API key is not configured");
		}
	}

	private long unitAmountInCents(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}

	private String qrCodeUrl(String paymentUrl) {
		if (paymentUrl == null || paymentUrl.isBlank()) {
			return null;
		}
		String encodedPaymentUrl = URLEncoder.encode(paymentUrl, StandardCharsets.UTF_8);
		return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=%s".formatted(encodedPaymentUrl);
	}

	private String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	public record CreatedEmbeddedPaymentIntent(String paymentIntentId, String clientSecret, String status) {
	}

	public record CreatedTipPaymentLink(String stripeId, String paymentUrl, String qrCodeUrl) {
	}

	public record CreatedTransactionPaymentLink(String stripeId, String paymentUrl) {
	}

	public record CreatedBusinessTopUpPaymentLink(String stripeId, String paymentUrl, String qrCodeUrl) {
	}

	public record CreatedTicketPaymentLink(String stripeId, String paymentUrl, String qrCodeUrl) {
	}
}
