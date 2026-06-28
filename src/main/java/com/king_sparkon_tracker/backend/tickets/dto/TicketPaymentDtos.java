package com.king_sparkon_tracker.backend.tickets.dto;

import jakarta.validation.constraints.NotBlank;

public final class TicketPaymentDtos {
    private TicketPaymentDtos() {}

    public record StripeCheckoutResponse(
            String paymentId,
            String checkoutSessionId,
            String checkoutUrl,
            String publishableKey
    ) {}

    public record CompleteStripeCheckoutRequest(
            @NotBlank String checkoutSessionId
    ) {}
}
