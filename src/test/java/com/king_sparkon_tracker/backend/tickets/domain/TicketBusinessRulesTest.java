package com.king_sparkon_tracker.backend.tickets.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TicketBusinessRulesTest {

    @Test
    void soldOutTicketCannotBePurchased() {
        assertThat(TicketBusinessRules.isSoldOut(10, 10)).isTrue();
        assertThatThrownBy(() -> TicketBusinessRules.requirePurchaseCapacity(10, 10, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough tickets");
    }

    @Test
    void positiveQuantityWithinCapacityCanBePurchased() {
        TicketBusinessRules.requirePurchaseCapacity(10, 4, 3);
        assertThat(TicketBusinessRules.calculateAvailable(10, 4)).isEqualTo(6);
    }

    @Test
    void nonPositiveTicketQuantityIsRejected() {
        assertThatThrownBy(() -> TicketBusinessRules.requirePurchaseCapacity(10, 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket quantity must be positive.");
        assertThatThrownBy(() -> TicketBusinessRules.requirePurchaseCapacity(10, 1, -2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket quantity must be positive.");
    }

    @Test
    void activeTicketCanBeVerified() {
        assertThat(TicketBusinessRules.canMarkUsed(UserTicketStatus.ACTIVE)).isTrue();
        assertThat(TicketBusinessRules.verificationMessage(UserTicketStatus.ACTIVE)).isEqualTo("Valid ticket. Entry approved.");
    }

    @Test
    void usedTicketCannotBeReused() {
        assertThat(TicketBusinessRules.canMarkUsed(UserTicketStatus.USED)).isFalse();
        assertThat(TicketBusinessRules.verificationMessage(UserTicketStatus.USED)).isEqualTo("Ticket already used.");
    }

    @Test
    void cancelledAndExpiredTicketsCannotBeVerified() {
        assertThat(TicketBusinessRules.canMarkUsed(UserTicketStatus.CANCELLED)).isFalse();
        assertThat(TicketBusinessRules.canMarkUsed(UserTicketStatus.EXPIRED)).isFalse();
        assertThat(TicketBusinessRules.verificationMessage(UserTicketStatus.CANCELLED)).isEqualTo("Ticket cancelled.");
        assertThat(TicketBusinessRules.verificationMessage(UserTicketStatus.EXPIRED)).isEqualTo("Ticket expired.");
    }

    @Test
    void availableCountIsCapacityMinusSoldWithZeroFloor() {
        assertThat(TicketBusinessRules.calculateAvailable(100, 35)).isEqualTo(65);
        assertThat(TicketBusinessRules.calculateAvailable(20, 25)).isZero();
    }

    @Test
    void percentAmountUsesExpectedMoneyScale() {
        BigDecimal fee = TicketBusinessRules.calculatePercentAmount(new BigDecimal("1000.00"), new BigDecimal("5.00"));
        assertThat(fee).isEqualByComparingTo("50.00");
    }

    @Test
    void nullPercentInputsReturnZeroMoney() {
        assertThat(TicketBusinessRules.calculatePercentAmount(null, new BigDecimal("5.00"))).isEqualByComparingTo("0.00");
        assertThat(TicketBusinessRules.calculatePercentAmount(new BigDecimal("1000.00"), null)).isEqualByComparingTo("0.00");
    }
}
