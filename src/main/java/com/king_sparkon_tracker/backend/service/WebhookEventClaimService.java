package com.king_sparkon_tracker.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.PayPalWebhookEvent;
import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;
import com.king_sparkon_tracker.backend.repository.PayPalWebhookEventRepository;
import com.king_sparkon_tracker.backend.repository.StripeWebhookEventRepository;

@Service
public class WebhookEventClaimService {

	public record Claim<T>(T event, boolean created) {
	}

	private final StripeWebhookEventRepository stripeEventRepository;
	private final PayPalWebhookEventRepository payPalEventRepository;
	private final WebhookEventInsertService insertService;

	public WebhookEventClaimService(
			StripeWebhookEventRepository stripeEventRepository,
			PayPalWebhookEventRepository payPalEventRepository,
			WebhookEventInsertService insertService) {
		this.stripeEventRepository = stripeEventRepository;
		this.payPalEventRepository = payPalEventRepository;
		this.insertService = insertService;
	}

	public Claim<StripeWebhookEvent> claimStripe(
			String eventId,
			String eventType,
			String subscriptionId,
			String rawPayload) {
		StripeWebhookEvent existing = stripeEventRepository.findByStripeEventId(eventId).orElse(null);
		if (existing != null) {
			return new Claim<>(existing, false);
		}

		try {
			return new Claim<>(insertService.insertStripe(eventId, eventType, subscriptionId, rawPayload), true);
		} catch (DataIntegrityViolationException duplicateClaim) {
			return new Claim<>(
					stripeEventRepository.findByStripeEventId(eventId)
							.orElseThrow(() -> duplicateClaim),
					false);
		}
	}

	public Claim<PayPalWebhookEvent> claimPayPal(
			String eventId,
			String eventType,
			String subscriptionId,
			String rawPayload) {
		PayPalWebhookEvent existing = payPalEventRepository.findByPaypalEventId(eventId).orElse(null);
		if (existing != null) {
			return new Claim<>(existing, false);
		}

		try {
			return new Claim<>(insertService.insertPayPal(eventId, eventType, subscriptionId, rawPayload), true);
		} catch (DataIntegrityViolationException duplicateClaim) {
			return new Claim<>(
					payPalEventRepository.findByPaypalEventId(eventId)
							.orElseThrow(() -> duplicateClaim),
					false);
		}
	}
}
