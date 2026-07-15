package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.dto.StripeWebhookResponse;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;
import com.king_sparkon_tracker.backend.model.StripeWebhookProcessingStatus;
import com.king_sparkon_tracker.backend.repository.StripeWebhookEventRepository;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

	@Mock
	private StripeBillingClient stripeBillingClient;

	@Mock
	private StripeService stripeService;

	@Mock
	private EmbeddedCartPaymentService embeddedCartPaymentService;

	@Mock
	private BusinessBillingService businessBillingService;

	@Mock
	private TransactionService transactionService;

	@Mock
	private TicketManagementService ticketManagementService;

	@Mock
	private BillingAuditService billingAuditService;

	@Mock
	private StripeWebhookEventRepository eventRepository;

	@Mock
	private WebhookEventClaimService eventClaimService;

	private StripeWebhookService service;

	@BeforeEach
	void setUp() {
		service = new StripeWebhookService(
				stripeBillingClient,
				stripeService,
				embeddedCartPaymentService,
				businessBillingService,
				transactionService,
				ticketManagementService,
				billingAuditService,
				eventRepository,
				eventClaimService,
				new ObjectMapper());
	}

	@Test
	void processVerifiedCheckoutSessionCompleted() throws Exception {
		String payload = checkoutPayload("evt_1", "cs_live_123", "sub_live_123");
		Event event = stripeEvent("evt_1", "checkout.session.completed");
		StripeWebhookEvent claimed = new StripeWebhookEvent(
				"evt_1", "checkout.session.completed", "sub_live_123", payload);
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventClaimService.claimStripe(
				"evt_1", "checkout.session.completed", "sub_live_123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(claimed, true));
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(payload, "sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		assertThat(response.eventId()).isEqualTo("evt_1");
		assertThat(response.stripeSubscriptionId()).isEqualTo("sub_live_123");
		verify(businessBillingService).handleStripeCheckoutSessionCompleted("cs_live_123", "sub_live_123", "evt_1");
	}

	@Test
	void processPaymentIntentSucceededMarksWebsitePaymentTransactionPaid() throws Exception {
		String payload = """
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
				""";
		Event event = stripeEvent("evt_txn_paid", "payment_intent.succeeded");
		StripeWebhookEvent claimed = new StripeWebhookEvent(
				"evt_txn_paid", "payment_intent.succeeded", null, payload);
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventClaimService.claimStripe(
				eq("evt_txn_paid"), eq("payment_intent.succeeded"), isNull(), eq(payload)))
				.thenReturn(new WebhookEventClaimService.Claim<>(claimed, true));
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(payload, "sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		verify(transactionService).handleWebsitePaymentSucceeded(88L, "pi_123", "evt_txn_paid");
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processEmbeddedCartPaymentIntentSucceededDispatchesVerifiedFulfilment() throws Exception {
		String payload = """
				{
				  "id": "evt_cart_paid",
				  "type": "payment_intent.succeeded",
				  "data": {
				    "object": {
				      "id": "pi_cart_123",
				      "metadata": {
				        "embeddedCart": "true"
				      }
				    }
				  }
				}
				""";
		Event event = stripeEvent("evt_cart_paid", "payment_intent.succeeded");
		PaymentIntent paymentIntent = mock(PaymentIntent.class);
		StripeWebhookEvent claimed = new StripeWebhookEvent(
				"evt_cart_paid", "payment_intent.succeeded", null, payload);
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventClaimService.claimStripe(
				eq("evt_cart_paid"), eq("payment_intent.succeeded"), isNull(), eq(payload)))
				.thenReturn(new WebhookEventClaimService.Claim<>(claimed, true));
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(stripeService.retrievePaymentIntent("pi_cart_123")).thenReturn(paymentIntent);

		StripeWebhookResponse response = service.process(payload, "sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		verify(embeddedCartPaymentService).handlePaymentIntentSucceeded(paymentIntent, "evt_cart_paid");
		verifyNoInteractions(transactionService, businessBillingService);
	}

	@Test
	void processDuplicateSkipsBusinessHandling() throws Exception {
		String payload = invoicePayload("evt_1", "invoice.payment_succeeded", "sub_live_123");
		Event event = stripeEvent("evt_1", "invoice.payment_succeeded");
		StripeWebhookEvent existing = mock(StripeWebhookEvent.class);
		when(existing.getStatus()).thenReturn(StripeWebhookProcessingStatus.PROCESSED);
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventClaimService.claimStripe(
				"evt_1", "invoice.payment_succeeded", "sub_live_123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(existing, false));

		StripeWebhookResponse response = service.process(payload, "sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.DUPLICATE);
		assertThat(response.message()).isEqualTo("Duplicate Stripe webhook skipped");
		verify(eventRepository, never()).save(any(StripeWebhookEvent.class));
		verifyNoInteractions(businessBillingService);
	}

	@Test
	void processInvalidSignaturePersistsFailedEventAndAudit() throws Exception {
		String payload = invoicePayload("evt_bad", "invoice.payment_failed", "sub_live_123");
		StripeWebhookEvent claimed = new StripeWebhookEvent(
				"evt_bad", "invoice.payment_failed", "sub_live_123", payload);
		when(stripeBillingClient.constructEvent(any(), eq("bad_signature")))
				.thenThrow(new SignatureVerificationException("bad signature", "bad_signature"));
		when(eventClaimService.claimStripe(
				"evt_bad", "invoice.payment_failed", "sub_live_123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(claimed, true));
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(payload, "bad_signature");

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
		String payload = invoicePayload("evt_failed", "invoice.payment_failed", "sub_live_123");
		Event event = stripeEvent("evt_failed", "invoice.payment_failed");
		StripeWebhookEvent claimed = new StripeWebhookEvent(
				"evt_failed", "invoice.payment_failed", "sub_live_123", payload);
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventClaimService.claimStripe(
				"evt_failed", "invoice.payment_failed", "sub_live_123", payload))
				.thenReturn(new WebhookEventClaimService.Claim<>(claimed, true));
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(payload, "sig_header");

		assertThat(response.status()).isEqualTo(StripeWebhookProcessingStatus.PROCESSED);
		verify(businessBillingService).handleStripeInvoicePaymentFailed("sub_live_123", "evt_failed");
	}

	@Test
	void processUnknownEventStoresAndIgnoresIt() throws Exception {
		String payload = """
				{
				  "id": "evt_unknown",
				  "type": "charge.succeeded",
				  "data": { "object": { "id": "ch_123" } }
				}
				""";
		Event event = stripeEvent("evt_unknown", "charge.succeeded");
		StripeWebhookEvent claimed = new StripeWebhookEvent(
				"evt_unknown", "charge.succeeded", null, payload);
		when(stripeBillingClient.constructEvent(any(), eq("sig_header"))).thenReturn(event);
		when(eventClaimService.claimStripe(
				eq("evt_unknown"), eq("charge.succeeded"), isNull(), eq(payload)))
				.thenReturn(new WebhookEventClaimService.Claim<>(claimed, true));
		when(eventRepository.save(any(StripeWebhookEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		StripeWebhookResponse response = service.process(payload, "sig_header");

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
