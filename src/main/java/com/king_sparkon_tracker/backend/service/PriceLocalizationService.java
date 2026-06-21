package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.config.PriceLocalizationProperties;
import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;

@Service
public class PriceLocalizationService {

	private static final int MONEY_SCALE = 2;

	private final PriceLocalizationProperties properties;
	private final TrackerUserService userService;

	public PriceLocalizationService(
			PriceLocalizationProperties properties,
			TrackerUserService userService) {
		this.properties = properties;
		this.userService = userService;
	}

	public MoneyResponse localize(BigDecimal zarAmount, String actorUsername) {
		TrackerUser user = userService.getUserByUsername(actorUsername);
		return localize(zarAmount, user);
	}

	public MoneyResponse localize(BigDecimal zarAmount, TrackerUser user) {
		LocalizationCountry localizationCountry = user == null || user.getLocalizationCountry() == null
				? LocalizationCountry.SOUTH_AFRICA
				: user.getLocalizationCountry();

		if (localizationCountry == LocalizationCountry.SOUTH_AFRICA) {
			return money(zarAmount, SupportedCurrency.ZAR);
		}

		return money(convertZarToUsd(zarAmount), SupportedCurrency.USD);
	}

	public MoneyResponse base(BigDecimal zarAmount) {
		return money(zarAmount, properties.getBaseCurrency());
	}

	private BigDecimal convertZarToUsd(BigDecimal zarAmount) {
		if (zarAmount == null) {
			return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		}

		BigDecimal rate = properties.getUsdToZarRate();

		if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalStateException("pricing.localization.usd-to-zar-rate must be greater than zero");
		}

		return zarAmount.divide(rate, MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private MoneyResponse money(BigDecimal amount, SupportedCurrency currency) {
		BigDecimal safeAmount = amount == null
				? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

		return new MoneyResponse(
				safeAmount,
				currency,
				symbol(currency),
				format(safeAmount, currency)
		);
	}

	private String symbol(SupportedCurrency currency) {
		return switch (currency) {
			case ZAR -> "R";
			case USD -> "$";
		};
	}

	private String format(BigDecimal amount, SupportedCurrency currency) {
		Locale locale = currency == SupportedCurrency.ZAR
				? Locale.forLanguageTag("en-ZA")
				: Locale.US;

		NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
		formatter.setCurrency(java.util.Currency.getInstance(currency.name()));
		formatter.setMinimumFractionDigits(MONEY_SCALE);
		formatter.setMaximumFractionDigits(MONEY_SCALE);

		return formatter.format(amount);
	}
}
