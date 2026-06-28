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
    void availableCountIsCapacityMinusSoldWithZeroFloor() {
        assertThat(TicketBusinessRules.calculateAvailable(100, 35)).isEqualTo(65);
        assertThat(TicketBusinessRules.calculateAvailable(20, 25)).isZero();
    }

    @Test
    void ticketWithdrawalFeeIsFivePercent() {
        BigDecimal fee = TicketBusinessRules.calculatePercentAmount(new BigDecimal("1000.00"), new BigDecimal("5.00"));
        assertThat(fee).isEqualByComparingTo("50.00");
    }
}
