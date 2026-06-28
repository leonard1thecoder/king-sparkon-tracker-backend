package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketEventComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventCommentRepository extends JpaRepository<TicketEventComment, String> {
    List<TicketEventComment> findByEventIdOrderByCreatedAtDesc(String eventId);
}
