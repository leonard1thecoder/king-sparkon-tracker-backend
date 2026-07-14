package com.king_sparkon_tracker.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.model.PayPalWebhookEvent;
import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;
import com.king_sparkon_tracker.backend.repository.PayPalWebhookEventRepository;
import com.king_sparkon_tracker.backend.repository.StripeWebhookEventRepository;

/**
 * Inserts provider webhook claims in isolated transactions so a uniqueness
 * conflict cannot mark the caller's business transaction rollback-only.
 */
@Service
public class WebhookEventInsertService {

	private final StripeWebhookEventRepository stripeEventRepository;
	private final PayPalWebhookEventRepository payPalEventRepository;

	public WebhookEventInsertService(
			StripeWebhookEventRepository stripeEventRepository,
			PayPalWebhookEventRepository payPalEventRepository) {
		this.stripeEventRepository = stripeEventRepository;
		this.payPalEventRepository = payPalEventRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public StripeWebhookEvent insertStripe(
			String eventId,
			String eventType,
			String subscriptionId,
			String rawPayload) {
		return stripeEventRepository.saveAndFlush(
				new StripeWebhookEvent(eventId, eventType, subscriptionId, rawPayload));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public PayPalWebhookEvent insertPayPal(
			String eventId,
			String eventType,
			String subscriptionId,
			String rawPayload) {
		return payPalEventRepository.saveAndFlush(
				new PayPalWebhookEvent(eventId, eventType, subscriptionId, rawPayload));
	}
}
