package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketAuditLogResponse;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/observability")
public class TicketObservabilityController {

    private final TicketManagementService ticketManagementService;

    public TicketObservabilityController(TicketManagementService ticketManagementService) {
        this.ticketManagementService = ticketManagementService;
    }

    @GetMapping("/entries")
    public List<TicketAuditLogResponse> viewEntries(@RequestParam Optional<String> ownerId, @RequestParam Optional<String> eventId) {
        return ticketManagementService.getLogs(ownerId, eventId);
    }
}
