package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAuditLogRepository extends JpaRepository<TicketAuditLog, String> {
    List<TicketAuditLog> findTop100ByOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<TicketAuditLog> findTop100ByEventIdOrderByCreatedAtDesc(String eventId);
    List<TicketAuditLog> findTop100ByOrderByCreatedAtDesc();
}
