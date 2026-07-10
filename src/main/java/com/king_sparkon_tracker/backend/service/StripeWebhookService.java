package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.StripeWebhookResponse;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;
import com.king_sparkon_tracker.backend.model.StripeWebhookProcessingStatus;
import com.king_sparkon_tracker.backend.repository.StripeWebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;

@Service
@Transactional
public class StripeWebhookService {

	private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

	private final StripeBillingClient stripeBillingClient;
	private final StripeService stripeService;
	private final EmbeddedCartPaymentService embeddedCartPaymentService;
	private final BusinessBillingService businessBillingService;
	private final TransactionService transactionService;
	private final BillingAuditService billingAuditService;
	private final StripeWebhookEventRepository eventRepository;
	private final ObjectMapper objectMapper;

	public StripeWebhookService(
			StripeBillingClient stripeBillingClient,
			StripeService stripeService,
			EmbeddedCartPaymentService embeddedCartPaymentService,
			BusinessBillingService businessBillingService,
			TransactionService transactionService,
			BillingAuditService billingAuditService,
			StripeWebhookEventRepository eventRepository,
			ObjectMapper objectMapper) {
		this.stripeBillingClient = stripeBillingClient;
		this.stripeService = stripeService;
		this.embeddedCartPaymentService = embeddedCartPaymentService;
		this.businessBillingService = businessBillingService;
		this.transactionService = transactionService;
		this.billingAuditService = billingAuditService;
		this.eventRepository = eventRepository;
		this.objectMapper = objectMapper;
	}

	public StripeWebhookResponse process(String rawPayload, String stripeSignature) {
		JsonNode payload;
		try {
			payload = objectMapper.readTree(rawPayload);
		} catch (Exception exception) {
			return new StripeWebhookResponse(null, null, null, StripeWebhookProcessingStatus.FAILED, "Stripe webhook payload is not valid JSON");
		}

		String eventId = text(payload, "id");
		String eventType = text(payload, "type");
		JsonNode object = payload.path("data").path("object");
		String stripeSubscriptionId = extractSubscriptionId(eventType, object);

		if (stripeSignature == null || stripeSignature.isBlank()) {
			return signatureFailed(eventId, eventType, stripeSubscriptionId, rawPayload, "Stripe-Signature header is missing");
		}

		try {
			Event event = stripeBillingClient.constructEvent(rawPayload, stripeSignature);
			eventId = event.getId();
			eventType = event.getType();
		} catch (SignatureVerificationException exception) {
			return signatureFailed(eventId, eventType, stripeSubscriptionId, rawPayload, "Stripe webhook signature verification failed");
		} catch (RuntimeException exception) {
			log.warn("stripe_webhook_verification_failed eventId={} type={} reason={}", eventId, eventType, exception.getMessage());
			return new StripeWebhookResponse(eventId, eventType, stripeSubscriptionId, StripeWebhookProcessingStatus.FAILED, exception.getMessage());
		}

		if (eventId == null || eventId.isBlank()) {
			return new StripeWebhookResponse(null, eventType, stripeSubscriptionId, StripeWebhookProcessingStatus.FAILED, "Stripe webhook event id is missing");
		}

		if (eventRepository.existsByStripeEventId(eventId)) {
			log.info("stripe_webhook_duplicate_skipped eventId={} type={} stripeSubscriptionId={}", eventId, eventType, stripeSubscriptionId);
			return new StripeWebhookResponse(eventId, eventType, stripeSubscriptionId, StripeWebhookProcessingStatus.DUPLICATE, "Duplicate Stripe webhook skipped");
		}

		StripeWebhookEvent webhookEvent = eventRepository.save(new StripeWebhookEvent(eventId, eventType, stripeSubscriptionId, rawPayload));

		try {
			boolean handled = handleEvent(eventType, object, eventId);
			if (handled) {
				webhookEvent.processed();
				eventRepository.save(webhookEvent);
				return new StripeWebhookResponse(eventId, eventType, stripeSubscriptionId, webhookEvent.getStatus(), "Webhook processed");
			}

			webhookEvent.ignored();
			eventRepository.save(webhookEvent);
			return new StripeWebhookResponse(eventId, eventType, stripeSubscriptionId, webhookEvent.getStatus(), "Webhook ignored");
		} catch (RuntimeException exception) {
			webhookEvent.failed(exception.getMessage());
			eventRepository.save(webhookEvent);
			log.warn("stripe_webhook_processing_failed eventId={} type={} stripeSubscriptionId={} reason={}",
					eventId, eventType, stripeSubscriptionId, exception.getMessage());
			return new StripeWebhookResponse(eventId, eventType, stripeSubscriptionId, webhookEvent.getStatus(), exception.getMessage());
		}
	}

	private boolean handleEvent(String eventType, JsonNode object, String eventId) {
		switch (eventType) {
			case "checkout.session.completed" -> {
				String transactionId = metadata(object, "transactionId");
				if (transactionId != null) {
					transactionService.handleWebsitePaymentSucceeded(
							longValue(transactionId, "Stripe transactionId metadata must be numeric"),
							firstText(text(object, "payment_intent"), text(object, "payment_link"), text(object, "id")),
							eventId);
					return true;
				}

				String subscriptionId = firstText(text(object, "subscription"), text(object.path("subscription_details"), "subscription"));
				if (subscriptionId == null) return false;
				businessBillingService.handleStripeCheckoutSessionCompleted(text(object, "id"), subscriptionId, eventId);
				return true;
			}
			case "payment_intent.succeeded" -> {
				if ("true".equalsIgnoreCase(metadata(object, "embeddedCart"))) {
					embeddedCartPaymentService.handlePaymentIntentSucceeded(
							stripeService.retrievePaymentIntent(text(object, "id")),
							eventId);
					return true;
				}

				String transactionId = metadata(object, "transactionId");
				if (transactionId == null) return false;
				transactionService.handleWebsitePaymentSucceeded(
						longValue(transactionId, "Stripe transactionId metadata must be numeric"),
						text(object, "id"),
						eventId);
				return true;
			}
			case "payment_intent.processing", "payment_intent.payment_failed", "payment_intent.canceled" -> {
				return "true".equalsIgnoreCase(metadata(object, "embeddedCart"));
			}
			case "invoice.payment_succeeded" -> {
				businessBillingService.handleStripeInvoicePaymentSucceeded(invoiceSubscriptionId(object), eventId);
				return true;
			}
			case "invoice.payment_failed" -> {
				businessBillingService.handleStripeInvoicePaymentFailed(invoiceSubscriptionId(object), eventId);
				return true;
			}
			case "customer.subscription.deleted" -> {
				businessBillingService.handleStripeSubscriptionCancelled(text(object, "id"), eventId);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private StripeWebhookResponse signatureFailed(
			String eventId,
			String eventType,
			String stripeSubscriptionId,
			String rawPayload,
			String reason) {
		if (eventId != null && !eventId.isBlank() && !eventRepository.existsByStripeEventId(eventId)) {
			StripeWebhookEvent failedEvent = new StripeWebhookEvent(eventId, eventType, stripeSubscriptionId, rawPayload);
			failedEvent.signatureFailed(reason);
			eventRepository.save(failedEvent);
		}

		billingAuditService.record(null, BillingAuditAction.WEBHOOK_SIGNATURE_FAILED, "stripe-webhook", eventId, stripeSubscriptionId, reason);
		log.warn("stripe_webhook_signature_failed eventId={} type={} stripeSubscriptionId={} reason={}", eventId, eventType, stripeSubscriptionId, reason);
		return new StripeWebhookResponse(eventId, eventType, stripeSubscriptionId, StripeWebhookProcessingStatus.SIGNATURE_FAILED, reason);
	}

	private String extractSubscriptionId(String eventType, JsonNode object) {
		if ("customer.subscription.deleted".equals(eventType)) return text(object, "id");
		if ("checkout.session.completed".equals(eventType)) {
			return firstText(text(object, "subscription"), text(object.path("subscription_details"), "subscription"));
		}
		if ("invoice.payment_succeeded".equals(eventType) || "invoice.payment_failed".equals(eventType)) {
			return invoiceSubscriptionId(object);
		}
		return null;
	}

	private String invoiceSubscriptionId(JsonNode object) {
		return firstText(
				text(object, "subscription"),
				text(object.path("parent").path("subscription_details"), "subscription"),
				text(object.path("subscription_details"), "subscription"));
	}

	private String firstText(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) return value;
		}
		return null;
	}

	private String metadata(JsonNode node, String fieldName) {
		return text(node == null ? null : node.path("metadata"), fieldName);
	}

	private Long longValue(String value, String message) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(message);
		}
	}

	private String text(JsonNode node, String fieldName) {
		JsonNode value = node == null ? null : node.get(fieldName);
		if (value == null || value.isNull()) return null;
		return value.asText();
	}
}
