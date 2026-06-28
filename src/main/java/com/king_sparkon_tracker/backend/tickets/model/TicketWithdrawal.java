package com.king_sparkon_tracker.backend.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ticket_withdrawals")
public class TicketWithdrawal {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal serviceFeePercent;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal serviceFeeAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TicketWithdrawalStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant processedAt;

    @Column(length = 700)
    private String notes;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) requestedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getServiceFeePercent() { return serviceFeePercent; }
    public void setServiceFeePercent(BigDecimal serviceFeePercent) { this.serviceFeePercent = serviceFeePercent; }
    public BigDecimal getServiceFeeAmount() { return serviceFeeAmount; }
    public void setServiceFeeAmount(BigDecimal serviceFeeAmount) { this.serviceFeeAmount = serviceFeeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public TicketWithdrawalStatus getStatus() { return status; }
    public void setStatus(TicketWithdrawalStatus status) { this.status = status; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
