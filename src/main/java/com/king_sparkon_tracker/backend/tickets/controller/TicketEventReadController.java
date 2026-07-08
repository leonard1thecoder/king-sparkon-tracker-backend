package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.service.TicketEventReadService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketEventReadController {

    private final TicketEventReadService ticketEventReadService;

    public TicketEventReadController(TicketEventReadService ticketEventReadService) {
        this.ticketEventReadService = ticketEventReadService;
    }

    @GetMapping("/events/all")
    public List<TicketEventResponse> getAllEvents() {
        return ticketEventReadService.getAllEvents();
    }
}
