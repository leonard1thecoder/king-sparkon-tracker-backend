package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEvent, String> {
    List<TicketEvent> findByStatusInAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(Collection<TicketEventStatus> statuses, LocalDate date);
    List<TicketEvent> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);
    List<TicketEvent> findByOwnerIdAndStatusInAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(String ownerId, Collection<TicketEventStatus> statuses, LocalDate date);
    long countByOwnerId(String ownerId);
    long countByOwnerIdAndStatusAndEventDateGreaterThanEqual(String ownerId, TicketEventStatus status, LocalDate date);
}
