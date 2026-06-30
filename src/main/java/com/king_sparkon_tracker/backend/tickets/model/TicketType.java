package com.king_sparkon_tracker.backend.tickets.model;

import java.util.Arrays;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TicketType {
    REGULAR,
    VIP,
    VVIP;

    @JsonCreator
    public static TicketType fromJson(String value) {
        if (value == null || value.isBlank()) {
            throw invalidTicketType(value);
        }

        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if ("GENERAL".equals(normalized) || "GENERAL_ADMISSION".equals(normalized)) {
            return REGULAR;
        }

        return Arrays.stream(values())
                .filter(ticketType -> ticketType.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> invalidTicketType(value));
    }

    public TicketType canonical() {
        return this;
    }

    private static IllegalArgumentException invalidTicketType(String value) {
        return new IllegalArgumentException(
                "Invalid ticket type '" + value + "'. Accepted values are REGULAR, VIP, VVIP, General and General Admission");
    }
}
