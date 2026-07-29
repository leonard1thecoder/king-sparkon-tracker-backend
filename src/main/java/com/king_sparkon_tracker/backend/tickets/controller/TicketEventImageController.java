package com.king_sparkon_tracker.backend.tickets.controller;

import java.security.Principal;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.king_sparkon_tracker.backend.service.GoogleStorageService;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.UpdateEventRequest;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;

@RestController
@RequestMapping("/api/v1/tickets/events")
public class TicketEventImageController {

    private final TicketManagementService ticketManagementService;
    private final GoogleStorageService googleStorageService;

    public TicketEventImageController(TicketManagementService ticketManagementService, GoogleStorageService googleStorageService) {
        this.ticketManagementService = ticketManagementService;
        this.googleStorageService = googleStorageService;
    }

    @PatchMapping(path = "/{eventId}/banner-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketEventResponse uploadBanner(@PathVariable String eventId, @RequestPart("file") MultipartFile file, Principal principal) {
        TicketEventResponse event = ticketManagementService.getEventById(eventId);
        GoogleStorageService.StoredImage image = googleStorageService.storeImage(file, "ticket-events", "owner-%s-event-%s".formatted(event.ownerId(), event.id()));
        return ticketManagementService.updateEvent(eventId, new UpdateEventRequest(null, null, null, null, null, image.url(), null, null, null, null, null), principal.getName());
    }
}
