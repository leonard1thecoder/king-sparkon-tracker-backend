package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.CreateEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.OwnerTicketDashboardResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PurchaseTicketsRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketPurchaseResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketWithdrawalRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketWithdrawalResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.UpdateEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.GateVerificationRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.GateVerificationResponse;
import com.king_sparkon_tracker.backend.tickets.service.TicketIdentityService;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;
import com.king_sparkon_tracker.backend.tickets.service.UserTicketResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketManagementService ticketManagementService;
    private final TicketIdentityService ticketIdentityService;

    public TicketController(TicketManagementService ticketManagementService, TicketIdentityService ticketIdentityService) {
        this.ticketManagementService = ticketManagementService;
        this.ticketIdentityService = ticketIdentityService;
    }

    @GetMapping("/events")
    public List<TicketEventResponse> getUpcomingEvents() { return ticketManagementService.getUpcomingEvents(); }
    @GetMapping("/events/{eventId}")
    public TicketEventResponse getEventById(@PathVariable String eventId) { return ticketManagementService.getEventById(eventId); }
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) { return ticketManagementService.createEvent(request); }
    @PatchMapping("/events/{eventId}")
    public TicketEventResponse updateEvent(@PathVariable String eventId, @Valid @RequestBody UpdateEventRequest request) { return ticketManagementService.updateEvent(eventId, request); }
    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketPurchaseResponse purchaseTickets(@Valid @RequestBody PurchaseTicketsRequest request) { return ticketManagementService.purchaseTickets(request); }
    @GetMapping("/my-tickets")
    public List<UserTicketResponse> getMyTickets(@RequestParam String userId) { return ticketManagementService.getMyTickets(userId); }
    @PostMapping("/verify/qr")
    public GateVerificationResponse verifyTicketByQr(@Valid @RequestBody GateVerificationRequest request, Principal principal) {
        return ticketIdentityService.verifyByQr(request, actorUsername(principal, request.workerId()));
    }
    @PostMapping("/verify/reference")
    public GateVerificationResponse verifyTicketByReference(@Valid @RequestBody GateVerificationRequest request, Principal principal) {
        return ticketIdentityService.verifyByReference(request, actorUsername(principal, request.workerId()));
    }
    @GetMapping("/owner/dashboard")
    public OwnerTicketDashboardResponse getOwnerDashboard(@RequestParam String ownerId) { return ticketManagementService.getOwnerDashboard(ownerId); }
    @GetMapping("/owner/events")
    public List<TicketEventResponse> getOwnerEvents(@RequestParam String ownerId) { return ticketManagementService.getOwnerEvents(ownerId); }
    @PostMapping("/owner/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketWithdrawalResponse requestWithdrawal(@Valid @RequestBody TicketWithdrawalRequest request) { return ticketManagementService.requestWithdrawal(request); }
    @GetMapping("/owner/withdrawals")
    public List<TicketWithdrawalResponse> getWithdrawals(@RequestParam String ownerId) { return ticketManagementService.getWithdrawals(ownerId); }

    private String actorUsername(Principal principal, String fallbackWorkerId) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (fallbackWorkerId != null && !fallbackWorkerId.isBlank()) {
            return fallbackWorkerId.trim();
        }
        throw new IllegalArgumentException("Authenticated worker is required.");
    }
}
