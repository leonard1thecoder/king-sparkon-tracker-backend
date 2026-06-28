package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventTicketTypeRepository extends JpaRepository<EventTicketType, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticketType from EventTicketType ticketType where ticketType.event.id = :eventId and ticketType.type = :type")
    Optional<EventTicketType> findLockedByEventIdAndType(@Param("eventId") String eventId, @Param("type") TicketType type);
}
