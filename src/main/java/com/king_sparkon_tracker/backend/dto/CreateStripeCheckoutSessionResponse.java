package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.SubscriptionPaymentStatus;

public record CreateStripeCheckoutSessionResponse(
		Long subscriptionId,
		String checkoutSessionId,
		String checkoutUrl,
		SubscriptionPaymentStatus paymentStatus
) {
}
