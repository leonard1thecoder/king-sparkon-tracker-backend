package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.PayPalWebhookProcessingStatus;

public record PayPalWebhookResponse(
		String eventId,
		String eventType,
		String paypalSubscriptionId,
		PayPalWebhookProcessingStatus status,
		String message
) {
}
