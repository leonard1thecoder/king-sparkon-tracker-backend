package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.dto.PayPalWebhookResponse;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.PayPalWebhookEvent;
import com.king_sparkon_tracker.backend.model.PayPalWebhookProcessingStatus;
import com.king_sparkon_tracker.backend.repository.PayPalWebhookEventRepository;

@ExtendWith(MockitoExtension.class)
class PayPalWebhookServiceTest {

	@Mock
	private PayPalBillingClient payPalBillingClient;

	@Mock
	private BusinessBillingService businessBillingService;

	@Mock
	private BillingAuditService billingAuditService;

	@Mock
	private PayPalWebhookEventRepository eventRepository;

	@Mock
	private WebhookEventClaimService eventClaimService;

	private PayPalWebhookService service;

	@BeforeEach
	void setUp() {
		service = new PayPalWebhookService(
				payPalBillingClient,
				businessBillingService,
				billingAuditService,
				eventRepository,
				eventClaimService,
				new ObjectMapper());
	}

	@Test
	void processVerifiedSubscriptionActivation() {
		String payload = payload("EVT-1", "BILLING.SUBSCRIPTION.ACTIVATED", "I-SUB-123");
		PayPalWebhookEvent event = new PayPalWebhookEvent(
				"EVT-1", "BILLING.SUBSCRIPTION.ACTIVATED", "I-SUB-123", payload);
		when(payPalBillingClient.verifyWebhookSignature(any(), any(), any(), any(), any(), any())).thenReturn(true);
		when(eventClaimService.claimPayPal(
				"EVT-1", "BILLING.SUBSCRIPTION.ACTIVATED", "I-SUB-123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(event, true));
		when(eventRepository.save(any(PayPalWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		PayPalWebhookResponse response = service.process(
				payload,
				"T-1",
				"2026-06-01T10:00:00Z",
				"https://paypal.example/cert",
				"SHA256withRSA",
				"signature");

		assertThat(response.status()).isEqualTo(PayPalWebhookProcessingStatus.PROCESSED);
		assertThat(response.eventId()).isEqualTo("EVT-1");
		assertThat(response.paypalSubscriptionId()).isEqualTo("I-SUB-123");
		verify(businessBillingService).handlePayPalSubscriptionActivated("I-SUB-123", "EVT-1");
	}

	@Test
	void processDuplicateSkipsBusinessHandling() {
		String payload = payload("EVT-1", "PAYMENT.SALE.COMPLETED", "I-SUB-123");
		PayPalWebhookEvent existing = new PayPalWebhookEvent(
				"EVT-1", "PAYMENT.SALE.COMPLETED", "I-SUB-123", payload);
		existing.processed();
		when(payPalBillingClient.verifyWebhookSignature(any(), any(), any(), any(), any(), any())).thenReturn(true);
		when(eventClaimService.claimPayPal(
				"EVT-1", "PAYMENT.SALE.COMPLETED", "I-SUB-123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(existing, false));

		PayPalWebhookResponse response = service.process(
				payload,
				"T-1",
				"2026-06-01T10:00:00Z",
				"https://paypal.example/cert",
				"SHA256withRSA",
				"signature");

		assertThat(response.status()).isEqualTo(PayPalWebhookProcessingStatus.DUPLICATE);
		assertThat(response.message()).isEqualTo("Duplicate PayPal webhook skipped");
		verify(eventRepository, never()).save(any(PayPalWebhookEvent.class));
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processInvalidSignaturePersistsFailedEventAndAudit() {
		String payload = payload("EVT-BAD", "PAYMENT.SALE.COMPLETED", "I-SUB-123");
		PayPalWebhookEvent event = new PayPalWebhookEvent(
				"EVT-BAD", "PAYMENT.SALE.COMPLETED", "I-SUB-123", payload);
		when(payPalBillingClient.verifyWebhookSignature(any(), any(), any(), any(), any(), any())).thenReturn(false);
		when(eventClaimService.claimPayPal(
				"EVT-BAD", "PAYMENT.SALE.COMPLETED", "I-SUB-123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(event, true));
		when(eventRepository.save(any(PayPalWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		PayPalWebhookResponse response = service.process(
				payload,
				"T-1",
				"2026-06-01T10:00:00Z",
				"https://paypal.example/cert",
				"SHA256withRSA",
				"bad-signature");

		assertThat(response.status()).isEqualTo(PayPalWebhookProcessingStatus.SIGNATURE_FAILED);
		assertThat(response.message()).isEqualTo("Signature verification failed");
		verify(billingAuditService).record(
				null,
				BillingAuditAction.WEBHOOK_SIGNATURE_FAILED,
				"paypal-webhook",
				"EVT-BAD",
				"I-SUB-123",
				"PayPal webhook signature verification failed");
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processMissingEventIdReturnsFailedResponse() {
		PayPalWebhookResponse response = service.process(
				"""
				{
				  "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
				  "resource": { "id": "I-SUB-123" }
				}
				""",
				"T-1",
				"2026-06-01T10:00:00Z",
				"https://paypal.example/cert",
				"SHA256withRSA",
				"signature");

		assertThat(response.status()).isEqualTo(PayPalWebhookProcessingStatus.FAILED);
		assertThat(response.message()).isEqualTo("PayPal webhook event id is missing");
		verifyNoInteractions(eventRepository, eventClaimService, businessBillingService, billingAuditService);
	}

	private String payload(String eventId, String eventType, String subscriptionId) {
		return """
				{
				  "id": "%s",
				  "event_type": "%s",
				  "resource": { "id": "%s" }
				}
				""".formatted(eventId, eventType, subscriptionId);
	}
}
