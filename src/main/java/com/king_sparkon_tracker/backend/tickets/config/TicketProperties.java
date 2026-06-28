package com.king_sparkon_tracker.backend.tickets.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tickets")
public record TicketProperties(
        BigDecimal withdrawalFeePercent,
        BigDecimal checkoutServiceFeePercent,
        BigDecimal withdrawalMinimumZar
) {
    public TicketProperties {
        if (withdrawalFeePercent == null) withdrawalFeePercent = new BigDecimal("5.00");
        if (checkoutServiceFeePercent == null) checkoutServiceFeePercent = BigDecimal.ZERO;
        if (withdrawalMinimumZar == null) withdrawalMinimumZar = new BigDecimal("100.00");
    }
}
