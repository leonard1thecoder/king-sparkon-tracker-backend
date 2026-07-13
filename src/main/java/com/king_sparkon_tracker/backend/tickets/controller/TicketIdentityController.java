package com.king_sparkon_tracker.backend.tickets.controller;

import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.TicketIdentityResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.TransferTicketRequest;
import com.king_sparkon_tracker.backend.tickets.service.TicketIdentityService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketIdentityController {

    private final TicketIdentityService ticketIdentityService;

    public TicketIdentityController(TicketIdentityService ticketIdentityService) {
        this.ticketIdentityService = ticketIdentityService;
    }

    @GetMapping("/my-tickets/current")
    public List<TicketIdentityResponse> getCurrentUserTickets(Principal principal) {
        return ticketIdentityService.getCurrentUserTickets(actorUsername(principal));
    }

    @PatchMapping(value = "/{ticketId}/verification-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketIdentityResponse saveVerificationPhoto(
            @PathVariable String ticketId,
            @RequestPart("file") MultipartFile file,
            Principal principal
    ) {
        return ticketIdentityService.saveVerificationPhoto(ticketId, file, actorUsername(principal));
    }

    @PostMapping("/{ticketId}/transfer")
    public TicketIdentityResponse transferTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TransferTicketRequest request,
            Principal principal
    ) {
        return ticketIdentityService.transferTicket(ticketId, request.username(), actorUsername(principal));
    }

    private String actorUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required.");
        }
        return principal.getName();
    }
}
