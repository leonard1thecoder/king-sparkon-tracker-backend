package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.king_sparkon_tracker.backend.model.PayPalWebhookEvent;
import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;
import com.king_sparkon_tracker.backend.repository.PayPalWebhookEventRepository;
import com.king_sparkon_tracker.backend.repository.StripeWebhookEventRepository;

class WebhookEventClaimServiceTest {

	private StripeWebhookEventRepository stripeRepository;
	private PayPalWebhookEventRepository payPalRepository;
	private WebhookEventInsertService insertService;
	private WebhookEventClaimService service;

	@BeforeEach
	void setUp() {
		stripeRepository = mock(StripeWebhookEventRepository.class);
		payPalRepository = mock(PayPalWebhookEventRepository.class);
		insertService = mock(WebhookEventInsertService.class);
		service = new WebhookEventClaimService(stripeRepository, payPalRepository, insertService);
	}

	@Test
	void returnsStripeWinnerWhenConcurrentInsertHitsUniqueConstraint() {
		StripeWebhookEvent winner = new StripeWebhookEvent("evt_1", "payment_intent.succeeded", null, "{}");
		when(stripeRepository.findByStripeEventId("evt_1"))
				.thenReturn(Optional.empty(), Optional.of(winner));
		when(insertService.insertStripe("evt_1", "payment_intent.succeeded", null, "{}"))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		WebhookEventClaimService.Claim<StripeWebhookEvent> claim = service.claimStripe(
				"evt_1", "payment_intent.succeeded", null, "{}");

		assertThat(claim.created()).isFalse();
		assertThat(claim.event()).isSameAs(winner);
	}

	@Test
	void returnsPayPalWinnerWhenConcurrentInsertHitsUniqueConstraint() {
		PayPalWebhookEvent winner = new PayPalWebhookEvent("WH-1", "PAYMENT.SALE.COMPLETED", "sub-1", "{}");
		when(payPalRepository.findByPaypalEventId("WH-1"))
				.thenReturn(Optional.empty(), Optional.of(winner));
		when(insertService.insertPayPal("WH-1", "PAYMENT.SALE.COMPLETED", "sub-1", "{}"))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		WebhookEventClaimService.Claim<PayPalWebhookEvent> claim = service.claimPayPal(
				"WH-1", "PAYMENT.SALE.COMPLETED", "sub-1", "{}");

		assertThat(claim.created()).isFalse();
		assertThat(claim.event()).isSameAs(winner);
	}
}
