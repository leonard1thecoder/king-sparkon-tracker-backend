package com.king_sparkon_tracker.backend.tickets.service;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.EventTicketTypeResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TicketEventReadService {

    private final TicketEventRepository ticketEventRepository;

    public TicketEventReadService(TicketEventRepository ticketEventRepository) {
        this.ticketEventRepository = ticketEventRepository;
    }

    public List<TicketEventResponse> getAllEvents() {
        Sort sort = Sort.by(
                Sort.Order.asc("eventDate"),
                Sort.Order.asc("eventTime"),
                Sort.Order.desc("updatedAt")
        );

        return ticketEventRepository.findAll(sort)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    private TicketEventResponse toEventResponse(TicketEvent event) {
        return new TicketEventResponse(
                event.getId(),
                event.getOwnerId(),
                event.getName(),
                event.getDescription(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getBannerUrl(),
                event.getPosterPhotoUrl(),
                event.getStatus(),
                event.getTicketTypes().stream()
                        .map(this::toTicketTypeResponse)
                        .sorted(Comparator.comparing(EventTicketTypeResponse::type))
                        .toList(),
                event.getWorkerIds() == null ? List.of() : List.copyOf(event.getWorkerIds()),
                event.getAffiliateIds() == null ? List.of() : List.copyOf(event.getAffiliateIds()),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private EventTicketTypeResponse toTicketTypeResponse(EventTicketType ticketType) {
        return new EventTicketTypeResponse(
                ticketType.getId(),
                ticketType.getEvent() == null ? null : ticketType.getEvent().getId(),
                ticketType.getType(),
                ticketType.getPrice(),
                ticketType.getCapacity(),
                ticketType.getSold(),
                ticketType.getAvailable()
        );
    }
}
