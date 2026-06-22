package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.StripeWebhookProcessingStatus;

public record StripeWebhookResponse(
		String eventId,
		String eventType,
		String stripeSubscriptionId,
		StripeWebhookProcessingStatus status,
		String message
) {
}
