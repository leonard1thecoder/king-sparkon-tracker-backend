package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketEventPromotion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventBoostRepository extends JpaRepository<TicketEventPromotion, String> {
    List<TicketEventPromotion> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<TicketEventPromotion> findByEventIdOrderByCreatedAtDesc(String eventId);
}
