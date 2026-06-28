package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PurchaseTicketsRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketPurchaseResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.UserTicketResponse;
import com.king_sparkon_tracker.backend.tickets.security.TicketSecurityContext;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
    public TicketPurchaseResponse purchase(@Valid @RequestBody PurchaseTicketsRequest request, Authentication authentication) {
        String userId = ticketSecurityContext.currentUserId(authentication);
        PurchaseTicketsRequest securedRequest = new PurchaseTicketsRequest(request.eventId(), userId, request.buyerName(), request.buyerEmail(), request.ticketType(), request.quantity());
        return ticketManagementService.purchaseTickets(securedRequest);
    }

    @GetMapping("/tickets")
    public List<UserTicketResponse> tickets(Authentication authentication) {
        return ticketManagementService.getMyTickets(ticketSecurityContext.currentUserId(authentication));
    }
}
