package com.king_sparkon_tracker.backend.tickets.model;

public enum TicketType {
    REGULAR,
    General,
    VIP,
    VVIP;

    public TicketType canonical() {
        return this == General ? REGULAR : this;
    }
}
