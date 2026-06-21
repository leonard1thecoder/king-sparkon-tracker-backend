package com.king_sparkon_tracker.backend.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.PayPalWebhookResponse;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.PayPalWebhookEvent;
import com.king_sparkon_tracker.backend.model.PayPalWebhookProcessingStatus;
import com.king_sparkon_tracker.backend.repository.PayPalWebhookEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class PayPalWebhookService {

	private final PayPalBillingClient payPalBillingClient;
	private final BusinessBillingService businessBillingService;
	private final BillingAuditService billingAuditService;
	private final PayPalWebhookEventRepository eventRepository;
	private final ObjectMapper objectMapper;

	public PayPalWebhookService(
			PayPalBillingClient payPalBillingClient,
			BusinessBillingService businessBillingService,
			BillingAuditService billingAuditService,
			PayPalWebhookEventRepository eventRepository,
			ObjectMapper objectMapper) {
		this.payPalBillingClient = payPalBillingClient;
		this.businessBillingService = businessBillingService;
		this.billingAuditService = billingAuditService;
		this.eventRepository = eventRepository;
		this.objectMapper = objectMapper;
	}

	public PayPalWebhookResponse process(
			String rawPayload,
			String transmissionId,
			String transmissionTime,
			String certUrl,
			String authAlgo,
			String transmissionSignature) {
		try {
			Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {
			});

			String eventId = stringValue(payload.get("id"));
			String eventType = stringValue(payload.get("event_type"));
			String paypalSubscriptionId = extractSubscriptionId(payload);

			if (eventId == null || eventId.isBlank()) {
				throw new IllegalArgumentException("PayPal webhook event id is missing");
			}

			boolean validSignature = payPalBillingClient.verifyWebhookSignature(
					rawPayload,
					transmissionId,
					transmissionTime,
					certUrl,
					authAlgo,
					transmissionSignature
			);

			if (!validSignature) {
				PayPalWebhookEvent failedEvent = new PayPalWebhookEvent(eventId, eventType, paypalSubscriptionId, rawPayload);
				failedEvent.signatureFailed("PayPal webhook signature verification failed");
				eventRepository.save(failedEvent);

				billingAuditService.record(
						null,
						BillingAuditAction.WEBHOOK_SIGNATURE_FAILED,
						"paypal-webhook",
						eventId,
						paypalSubscriptionId,
						"PayPal webhook signature verification failed"
				);

				return new PayPalWebhookResponse(
						eventId,
						eventType,
						paypalSubscriptionId,
						PayPalWebhookProcessingStatus.SIGNATURE_FAILED,
						"Signature verification failed"
				);
			}

			if (eventRepository.existsByPaypalEventId(eventId)) {
				return new PayPalWebhookResponse(
						eventId,
						eventType,
						paypalSubscriptionId,
						PayPalWebhookProcessingStatus.DUPLICATE,
						"Duplicate PayPal webhook skipped"
				);
			}

			PayPalWebhookEvent event = eventRepository.save(new PayPalWebhookEvent(
					eventId,
					eventType,
					paypalSubscriptionId,
					rawPayload
			));

			handleEvent(eventType, paypalSubscriptionId, eventId);
			event.processed();
			eventRepository.save(event);

			return new PayPalWebhookResponse(
					eventId,
					eventType,
					paypalSubscriptionId,
					event.getStatus(),
					"Webhook processed"
			);
		} catch (Exception exception) {
			return new PayPalWebhookResponse(
					null,
					null,
					null,
					PayPalWebhookProcessingStatus.FAILED,
					exception.getMessage()
			);
		}
	}

	private void handleEvent(String eventType, String paypalSubscriptionId, String eventId) {
		switch (eventType) {
			case "BILLING.SUBSCRIPTION.ACTIVATED",
					"BILLING.SUBSCRIPTION.RE-ACTIVATED" ->
					businessBillingService.handlePayPalSubscriptionActivated(paypalSubscriptionId, eventId);

			case "PAYMENT.SALE.COMPLETED" ->
					businessBillingService.handlePayPalPaymentCompleted(paypalSubscriptionId, eventId);

			case "BILLING.SUBSCRIPTION.PAYMENT.FAILED" ->
					businessBillingService.handlePayPalPaymentFailed(paypalSubscriptionId, eventId);

			case "BILLING.SUBSCRIPTION.CANCELLED" ->
					businessBillingService.handlePayPalSubscriptionCancelled(paypalSubscriptionId, eventId);

			case "BILLING.SUBSCRIPTION.SUSPENDED" ->
					businessBillingService.handlePayPalSubscriptionSuspended(paypalSubscriptionId, eventId);

			case "BILLING.SUBSCRIPTION.EXPIRED" ->
					businessBillingService.handlePayPalSubscriptionExpired(paypalSubscriptionId, eventId);

			default -> {
				// ignored by design; event is still saved for traceability
			}
		}
	}

	@SuppressWarnings("unchecked")
	private String extractSubscriptionId(Map<String, Object> payload) {
		Object resourceObject = payload.get("resource");

		if (!(resourceObject instanceof Map<?, ?> rawResource)) {
			return null;
		}

		Map<String, Object> resource = (Map<String, Object>) rawResource;

		String subscriptionId = firstText(
				stringValue(resource.get("billing_agreement_id")),
				stringValue(resource.get("subscription_id")),
				stringValue(resource.get("id"))
		);

		return subscriptionId;
	}

	private String firstText(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}

		return null;
	}

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
