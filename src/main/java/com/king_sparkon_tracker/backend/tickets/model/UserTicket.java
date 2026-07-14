package com.king_sparkon_tracker.backend.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_tickets")
public class UserTicket {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 160)
    private String buyerName;

    @Column(nullable = false, length = 180)
    private String buyerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType ticketType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal pricePaid;

    @Column(length = 1024)
    private String qrCodeValue;

    @Column(nullable = false, unique = true, length = 64)
    private String ticketReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserTicketStatus status;

    @Column(nullable = false)
    private Instant purchasedAt;

    private Instant usedAt;

    @Column(length = 64)
    private String scannedByWorkerId;

    @Column(length = 512)
    private String verificationPhotoObjectName;

    private Instant verificationPhotoCapturedAt;

    private Instant transferredAt;

    @Column(length = 64)
    private String transferredFromUserId;

    @Column(nullable = false)
    private Integer ownershipVersion;

    @Version
    private long version;

    @PrePersist
    void prePersist() {
        if (purchasedAt == null) purchasedAt = Instant.now();
        if (ownershipVersion == null || ownershipVersion < 1) ownershipVersion = 1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public TicketType getTicketType() { return ticketType; }
    public void setTicketType(TicketType ticketType) { this.ticketType = ticketType; }
    public BigDecimal getPricePaid() { return pricePaid; }
    public void setPricePaid(BigDecimal pricePaid) { this.pricePaid = pricePaid; }
    public String getQrCodeValue() { return qrCodeValue; }
    public void setQrCodeValue(String qrCodeValue) { this.qrCodeValue = qrCodeValue; }
    public String getTicketReference() { return ticketReference; }
    public void setTicketReference(String ticketReference) { this.ticketReference = ticketReference; }
    public UserTicketStatus getStatus() { return status; }
    public void setStatus(UserTicketStatus status) { this.status = status; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Instant purchasedAt) { this.purchasedAt = purchasedAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public String getScannedByWorkerId() { return scannedByWorkerId; }
    public void setScannedByWorkerId(String scannedByWorkerId) { this.scannedByWorkerId = scannedByWorkerId; }
    public String getVerificationPhotoObjectName() { return verificationPhotoObjectName; }
    public void setVerificationPhotoObjectName(String verificationPhotoObjectName) { this.verificationPhotoObjectName = verificationPhotoObjectName; }
    public Instant getVerificationPhotoCapturedAt() { return verificationPhotoCapturedAt; }
    public void setVerificationPhotoCapturedAt(Instant verificationPhotoCapturedAt) { this.verificationPhotoCapturedAt = verificationPhotoCapturedAt; }
    public Instant getTransferredAt() { return transferredAt; }
    public void setTransferredAt(Instant transferredAt) { this.transferredAt = transferredAt; }
    public String getTransferredFromUserId() { return transferredFromUserId; }
    public void setTransferredFromUserId(String transferredFromUserId) { this.transferredFromUserId = transferredFromUserId; }
    public Integer getOwnershipVersion() { return ownershipVersion; }
    public void setOwnershipVersion(Integer ownershipVersion) { this.ownershipVersion = ownershipVersion; }
    public long getVersion() { return version; }
}
