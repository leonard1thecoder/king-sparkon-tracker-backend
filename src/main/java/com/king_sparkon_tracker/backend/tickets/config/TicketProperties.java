package com.king_sparkon_tracker.backend.tickets.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.tickets")
public class TicketProperties {
    private BigDecimal withdrawalFeePercent = new BigDecimal("5.00");
    private BigDecimal checkoutServiceFeePercent = BigDecimal.ZERO;
    private BigDecimal withdrawalMinimumZar = new BigDecimal("100.00");
    private BigDecimal promotionPriceZar = new BigDecimal("1500.00");

    public BigDecimal withdrawalFeePercent() {
        return withdrawalFeePercent;
    }

    public void setWithdrawalFeePercent(BigDecimal withdrawalFeePercent) {
        this.withdrawalFeePercent = withdrawalFeePercent;
    }

    public BigDecimal checkoutServiceFeePercent() {
        return checkoutServiceFeePercent;
    }

    public void setCheckoutServiceFeePercent(BigDecimal checkoutServiceFeePercent) {
        this.checkoutServiceFeePercent = checkoutServiceFeePercent;
    }

    public BigDecimal withdrawalMinimumZar() {
        return withdrawalMinimumZar;
    }

    public void setWithdrawalMinimumZar(BigDecimal withdrawalMinimumZar) {
        this.withdrawalMinimumZar = withdrawalMinimumZar;
    }

    public BigDecimal promotionPriceZar() {
        return promotionPriceZar;
    }

    public void setPromotionPriceZar(BigDecimal promotionPriceZar) {
        this.promotionPriceZar = promotionPriceZar;
    }
}
