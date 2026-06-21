package com.king_sparkon_tracker.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.king_sparkon_tracker.backend.model.SupportedCurrency;

@Component
@ConfigurationProperties(prefix = "pricing.localization")
public class PriceLocalizationProperties {

	private SupportedCurrency baseCurrency = SupportedCurrency.ZAR;

	private SupportedCurrency internationalCurrency = SupportedCurrency.USD;

	private BigDecimal usdToZarRate = new BigDecimal("18.50");

	public SupportedCurrency getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(SupportedCurrency baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public SupportedCurrency getInternationalCurrency() {
		return internationalCurrency;
	}

	public void setInternationalCurrency(SupportedCurrency internationalCurrency) {
		this.internationalCurrency = internationalCurrency;
	}

	public BigDecimal getUsdToZarRate() {
		return usdToZarRate;
	}

	public void setUsdToZarRate(BigDecimal usdToZarRate) {
		this.usdToZarRate = usdToZarRate;
	}
}
