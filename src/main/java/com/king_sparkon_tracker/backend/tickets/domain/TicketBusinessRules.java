package com.king_sparkon_tracker.backend.tickets.domain;

import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TicketBusinessRules {
    private TicketBusinessRules() {
    }

    public static int calculateAvailable(int capacity, int sold) {
        return Math.max(capacity - sold, 0);
    }

    public static boolean isSoldOut(int capacity, int sold) {
        return sold >= capacity;
    }

    public static void requirePurchaseCapacity(int capacity, int sold, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Ticket quantity must be positive.");
        }
        int available = calculateAvailable(capacity, sold);
        if (isSoldOut(capacity, sold) || requestedQuantity > available) {
            throw new IllegalStateException("Not enough tickets are available for this class.");
        }
    }

    public static boolean canMarkUsed(UserTicketStatus status) {
        return status == UserTicketStatus.ACTIVE;
    }

    public static String verificationMessage(UserTicketStatus status) {
        return switch (status) {
            case ACTIVE -> "Valid ticket. Entry approved.";
            case USED -> "Ticket already used.";
            case CANCELLED -> "Ticket cancelled.";
            case EXPIRED -> "Ticket expired.";
        };
    }

    public static BigDecimal calculatePercentAmount(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
