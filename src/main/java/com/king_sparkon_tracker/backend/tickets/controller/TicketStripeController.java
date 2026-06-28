package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PurchaseTicketsRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketPaymentDtos.StripeCheckoutResponse;
import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.service.TicketStripeCheckoutService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/stripe")
public class TicketStripeController {
    private final TicketStripeCheckoutService stripeCheckoutService;

    public TicketStripeController(TicketStripeCheckoutService stripeCheckoutService) {
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @PostMapping("/checkout-demo")
    public StripeCheckoutResponse checkoutDemo(@Valid @RequestBody PurchaseTicketsRequest request) {
        BigDecimal sandboxAmount = BigDecimal.valueOf(100);
        var session = stripeCheckoutService.createTicketCheckoutSession("sandbox-ticket-payment", "King Sparkon Ticket Sandbox", TicketType.REGULAR, 1, sandboxAmount);
        return new StripeCheckoutResponse("sandbox-ticket-payment", session.getId(), session.getUrl(), stripeCheckoutService.publishableKey());
    }
}
