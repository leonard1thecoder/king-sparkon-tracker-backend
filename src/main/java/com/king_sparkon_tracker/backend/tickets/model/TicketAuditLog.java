package com.king_sparkon_tracker.backend.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ticket_audit_logs")
public class TicketAuditLog {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 64)
    private String ownerId;

    @Column(length = 64)
    private String eventId;

    @Column(length = 64)
    private String ticketId;

    @Column(length = 64)
    private String actorId;

    @Column(nullable = false, length = 80)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketAuditLevel level;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 3000)
    private String structuredMetadata;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public TicketAuditLevel getLevel() { return level; }
    public void setLevel(TicketAuditLevel level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStructuredMetadata() { return structuredMetadata; }
    public void setStructuredMetadata(String structuredMetadata) { this.structuredMetadata = structuredMetadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
