package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.CreateEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.OwnerTicketDashboardResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PurchaseTicketsRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketPurchaseResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketVerificationResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketWithdrawalRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketWithdrawalResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.UpdateEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.VerifyTicketRequest;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;
import com.king_sparkon_tracker.backend.tickets.service.UserTicketResponse;
import jakarta.validation.Valid;
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

    public TicketController(TicketManagementService ticketManagementService) {
        this.ticketManagementService = ticketManagementService;
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
    public TicketVerificationResponse verifyTicketByQr(@Valid @RequestBody VerifyTicketRequest request) { return ticketManagementService.verifyByQr(request.value(), request.workerId()); }
    @PostMapping("/verify/reference")
    public TicketVerificationResponse verifyTicketByReference(@Valid @RequestBody VerifyTicketRequest request) { return ticketManagementService.verifyByReference(request.value(), request.workerId()); }
    @GetMapping("/owner/dashboard")
    public OwnerTicketDashboardResponse getOwnerDashboard(@RequestParam String ownerId) { return ticketManagementService.getOwnerDashboard(ownerId); }
    @GetMapping("/owner/events")
    public List<TicketEventResponse> getOwnerEvents(@RequestParam String ownerId) { return ticketManagementService.getOwnerEvents(ownerId); }
    @PostMapping("/owner/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketWithdrawalResponse requestWithdrawal(@Valid @RequestBody TicketWithdrawalRequest request) { return ticketManagementService.requestWithdrawal(request); }
    @GetMapping("/owner/withdrawals")
    public List<TicketWithdrawalResponse> getWithdrawals(@RequestParam String ownerId) { return ticketManagementService.getWithdrawals(ownerId); }
}
