package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.dto.StripeWebhookResponse;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;
import com.king_sparkon_tracker.backend.model.StripeWebhookProcessingStatus;
import com.king_sparkon_tracker.backend.repository.StripeWebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

	@Mock
	private StripeBillingClient stripeBillingClient;

	@Mock
	private BusinessBillingService businessBillingService;

	@Mock
	private TransactionService transactionService;

	@Mock
	private BillingAuditService billingAuditService;

	@Mock
	private StripeWebhookEventRepository eventRepository;

	private StripeWebhookService service;

	@BeforeEach
	void setUp() {
		service = new StripeWebhookService(
				stripeBillingClient,
				businessBillingService,
				transactionService,
				billingAuditService,
				eventRepository,
				new ObjectMapper());
	}

	@Test
	void processVerifiedCheckoutSessionCompleted() throws Exception {
		Event event = stripeEvent("evt_1", "checkout.session.completed");
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventRepository.existsByStripeEventId("evt_1")).thenReturn(false);
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(
				checkoutPayload("evt_1", "cs_live_123", "sub_live_123"),
				"sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		assertThat(response.eventId()).isEqualTo("evt_1");
		assertThat(response.stripeSubscriptionId()).isEqualTo("sub_live_123");
		verify(businessBillingService).handleStripeCheckoutSessionCompleted("cs_live_123", "sub_live_123", "evt_1");
	}

	@Test
	void processPaymentIntentSucceededMarksWebsitePaymentTransactionPaid() throws Exception {
		Event event = stripeEvent("evt_txn_paid", "payment_intent.succeeded");
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventRepository.existsByStripeEventId("evt_txn_paid")).thenReturn(false);
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(
				"""
				{
				  "id": "evt_txn_paid",
				  "type": "payment_intent.succeeded",
				  "data": {
				    "object": {
				      "id": "pi_123",
				      "metadata": {
				        "transactionId": "88"
				      }
				    }
				  }
				}
				""",
				"sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		verify(transactionService).handleWebsitePaymentSucceeded(88L, "pi_123", "evt_txn_paid");
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processDuplicateSkipsBusinessHandling() throws Exception {
		Event event = stripeEvent("evt_1", "invoice.payment_succeeded");
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventRepository.existsByStripeEventId("evt_1")).thenReturn(true);

		StripeWebhookResponse response = service.process(
				invoicePayload("evt_1", "invoice.payment_succeeded", "sub_live_123"),
				"sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.DUPLICATE);
		assertThat(response.message()).isEqualTo("Duplicate Stripe webhook skipped");
		verify(eventRepository, never()).save(any(StripeWebhookEvent.class));
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processInvalidSignaturePersistsFailedEventAndAudit() throws Exception {
		when(stripeBillingClient.constructEvent(any(), eq("bad_signature")))
				.thenThrow(new SignatureVerificationException("bad signature", "bad_signature"));
		when(eventRepository.existsByStripeEventId("evt_bad")).thenReturn(false);
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(
				invoicePayload("evt_bad", "invoice.payment_failed", "sub_live_123"),
				"bad_signature");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.SIGNATURE_FAILED);
		assertThat(response.message()).isEqualTo("Stripe webhook signature verification failed");
		verify(billingAuditService).record(
				null,
				BillingAuditAction.WEBHOOK_SIGNATURE_FAILED,
				"stripe-webhook",
				"evt_bad",
				"sub_live_123",
				"Stripe webhook signature verification failed");
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processPaymentFailedDispatchesToBillingService() throws Exception {
		Event event = stripeEvent("evt_failed", "invoice.payment_failed");
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventRepository.existsByStripeEventId("evt_failed")).thenReturn(false);
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(
				invoicePayload("evt_failed", "invoice.payment_failed", "sub_live_123"),
				"sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		verify(businessBillingService).handleStripeInvoicePaymentFailed("sub_live_123", "evt_failed");
	}

	@Test
	void processUnknownEventStoresAndIgnoresIt() throws Exception {
		Event event = stripeEvent("evt_unknown", "charge.succeeded");
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventRepository.existsByStripeEventId("evt_unknown")).thenReturn(false);
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(
				"""
				{
				  "id": "evt_unknown",
				  "type": "charge.succeeded",
				  "data": { "object": { "id": "ch_123" } }
				}
				""",
				"sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.IGNORED);
		verifyNoInteractions(businessBillingService);
	}

	private Event stripeEvent(String eventId, String eventType) {
		Event event = mock(Event.class);
		when(event.getId()).thenReturn(eventId);
		when(event.getType()).thenReturn(eventType);
		return event;
	}

	private String checkoutPayload(String eventId, String checkoutSessionId, String subscriptionId) {
		return """
				{
				  "id": "%s",
				  "type": "checkout.session.completed",
				  "data": {
				    "object": {
				      "id": "%s",
				      "subscription": "%s"
				    }
				  }
				}
				""".formatted(eventId, checkoutSessionId, subscriptionId);
	}

	private String invoicePayload(String eventId, String eventType, String subscriptionId) {
		return """
				{
				  "id": "%s",
				  "type": "%s",
				  "data": {
				    "object": {
				      "id": "in_123",
				      "subscription": "%s"
				    }
				  }
				}
				""".formatted(eventId, eventType, subscriptionId);
	}
}
