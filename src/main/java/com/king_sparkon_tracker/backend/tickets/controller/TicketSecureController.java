package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.idempotency.IdempotentRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PromoteTicketEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PurchaseTicketsRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventPromotionResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketPurchaseResponse;
import com.king_sparkon_tracker.backend.tickets.security.TicketSecurityContext;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;
import com.king_sparkon_tracker.backend.tickets.service.UserTicketResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/me")
public class TicketSecureController {
    private final TicketManagementService ticketManagementService;
    private final TicketSecurityContext ticketSecurityContext;

    public TicketSecureController(TicketManagementService ticketManagementService, TicketSecurityContext ticketSecurityContext) {
        this.ticketManagementService = ticketManagementService;
        this.ticketSecurityContext = ticketSecurityContext;
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    @IdempotentRequest(scope = "ticket-purchase")
    public TicketPurchaseResponse purchase(@Valid @RequestBody PurchaseTicketsRequest request, Authentication authentication) {
        ticketSecurityContext.currentUserId(authentication);
        return ticketManagementService.purchaseTickets(request, authentication.getName());
    }

    @GetMapping("/tickets")
    public List<UserTicketResponse> tickets(Authentication authentication) {
        return ticketManagementService.getMyTickets(ticketSecurityContext.currentUserId(authentication));
    }

    @PostMapping("/events/{eventId}/boosts")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketEventPromotionResponse boostEvent(
            @PathVariable String eventId,
            @Valid @RequestBody PromoteTicketEventRequest request,
            Authentication authentication) {
        return ticketManagementService.promoteEvent(eventId, request, authentication.getName());
    }

    @GetMapping("/event-boosts")
    public List<TicketEventPromotionResponse> eventBoosts(Authentication authentication) {
        ticketSecurityContext.currentOwnerId(authentication);
        return ticketManagementService.getOwnerEventPromotions(authentication.getName());
    }
}
