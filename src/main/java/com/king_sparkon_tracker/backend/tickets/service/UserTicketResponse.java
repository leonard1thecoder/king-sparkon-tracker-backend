package com.king_sparkon_tracker.backend.tickets.service;

public class UserTicketResponse {
    public final Object id;
    public final Object eventId;
    public final Object userId;
    public final Object buyerName;
    public final Object buyerEmail;
    public final Object ticketType;
    public final Object pricePaid;
    public final Object qrCodeValue;
    public final Object ticketReference;
    public final Object status;
    public final Object purchasedAt;
    public final Object usedAt;
    public final Object scannedByWorkerId;

    public UserTicketResponse(Object id, Object eventId, Object userId, Object buyerName, Object buyerEmail, Object ticketType, Object pricePaid, Object qrCodeValue, Object ticketReference, Object status, Object purchasedAt, Object usedAt, Object scannedByWorkerId) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.ticketType = ticketType;
        this.pricePaid = pricePaid;
        this.qrCodeValue = qrCodeValue;
        this.ticketReference = ticketReference;
        this.status = status;
        this.purchasedAt = purchasedAt;
        this.usedAt = usedAt;
        this.scannedByWorkerId = scannedByWorkerId;
    }
}
