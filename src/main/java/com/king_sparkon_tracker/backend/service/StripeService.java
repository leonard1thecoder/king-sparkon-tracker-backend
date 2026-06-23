package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Tip;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentLinkCreateParams;

@Service
public class StripeService {

	private final String apiKey;

	public StripeService(@Value("${stripe.api-key:${stripe.secret-key:}}") String apiKey) {
		this.apiKey = apiKey;
	}

	public CreatedTipPaymentLink createTipPaymentLink(
			Tip tip,
			BigDecimal systemFee,
			BigDecimal netAmount,
			String callbackUrl) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("Stripe API key is not configured");
		}

		try {
			PaymentLink paymentLink = PaymentLink.create(
					paymentLinkParams(tip, systemFee, netAmount, callbackUrl),
					RequestOptions.builder()
							.setApiKey(apiKey)
							.build());

			if (paymentLink.getId() == null || paymentLink.getUrl() == null) {
				throw new IllegalStateException("Stripe payment link creation failed");
			}

			return new CreatedTipPaymentLink(
					paymentLink.getId(),
					paymentLink.getUrl(),
					qrCodeUrl(paymentLink.getUrl()));
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe payment link creation failed", exception);
		}
	}

	public CreatedTransactionPaymentLink createTransactionPaymentLink(InventoryTransaction transaction) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("Stripe API key is not configured");
		}

		try {
			PaymentLink paymentLink = PaymentLink.create(
					transactionPaymentLinkParams(transaction),
					RequestOptions.builder()
							.setApiKey(apiKey)
							.build());

			if (paymentLink.getId() == null || paymentLink.getUrl() == null) {
				throw new IllegalStateException("Stripe transaction payment link creation failed");
			}

			return new CreatedTransactionPaymentLink(paymentLink.getId(), paymentLink.getUrl());
		} catch (StripeException exception) {
			throw new IllegalStateException("Stripe transaction payment link creation failed", exception);
		}
	}

	private PaymentLinkCreateParams paymentLinkParams(
			Tip tip,
			BigDecimal systemFee,
			BigDecimal netAmount,
			String callbackUrl) {
		Map<String, String> metadata = Map.of(
				"tipId", String.valueOf(tip.getId()),
				"workerId", String.valueOf(tip.getWorkerId()),
				"systemFee", systemFee.toPlainString(),
				"netAmount", netAmount.toPlainString()
		);

		PaymentLinkCreateParams.Builder builder = PaymentLinkCreateParams.builder()
				.putAllMetadata(metadata)
				.setPaymentIntentData(PaymentLinkCreateParams.PaymentIntentData.builder()
						.putAllMetadata(metadata)
						.build())
				.addLineItem(PaymentLinkCreateParams.LineItem.builder()
						.setQuantity(1L)
						.setPriceData(PaymentLinkCreateParams.LineItem.PriceData.builder()
								.setCurrency("zar")
								.setUnitAmount(unitAmountInCents(tip.getTipAmount()))
								.setProductData(PaymentLinkCreateParams.LineItem.PriceData.ProductData.builder()
										.setName("King Sparkon worker tip")
										.build())
								.build())
						.build());

		if (callbackUrl != null && !callbackUrl.isBlank()) {
			builder.setAfterCompletion(PaymentLinkCreateParams.AfterCompletion.builder()
					.setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
					.setRedirect(PaymentLinkCreateParams.AfterCompletion.Redirect.builder()
							.setUrl(callbackUrl.trim())
							.build())
					.build());
		}

		return builder.build();
	}

	private PaymentLinkCreateParams transactionPaymentLinkParams(InventoryTransaction transaction) {
		BigDecimal totalAmount = transaction.getTotalAmount();
		if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transaction total must be greater than zero for website payment");
		}

		Map<String, String> metadata = Map.of(
				"transactionId", String.valueOf(transaction.getId()),
				"businessId", String.valueOf(transaction.getBusiness() == null ? "" : transaction.getBusiness().getId()),
				"paymentEmail", transaction.getPaymentEmail() == null ? "" : transaction.getPaymentEmail()
		);

		return PaymentLinkCreateParams.builder()
				.putAllMetadata(metadata)
				.setPaymentIntentData(PaymentLinkCreateParams.PaymentIntentData.builder()
						.putAllMetadata(metadata)
						.build())
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

	private long unitAmountInCents(BigDecimal amount) {
		return amount
				.setScale(2, RoundingMode.HALF_UP)
				.movePointRight(2)
				.setScale(0, RoundingMode.HALF_UP)
				.longValueExact();
	}

	private String qrCodeUrl(String paymentUrl) {
		String encodedPaymentUrl = URLEncoder.encode(paymentUrl, StandardCharsets.UTF_8);
		return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=%s"
				.formatted(encodedPaymentUrl);
	}

	public record CreatedTipPaymentLink(
			String stripeId,
			String paymentUrl,
			String qrCodeUrl
	) {
	}

	public record CreatedTransactionPaymentLink(
			String stripeId,
			String paymentUrl
	) {
	}
}
