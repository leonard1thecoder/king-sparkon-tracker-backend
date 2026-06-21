package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;

import com.king_sparkon_tracker.backend.model.SupportedCurrency;

import io.swagger.v3.oas.annotations.media.Schema;

public record MoneyResponse(
		@Schema(description = "Money amount.", example = "20.50")
		BigDecimal amount,

		@Schema(description = "ISO currency code.", example = "ZAR")
		SupportedCurrency currency,

		@Schema(description = "Currency symbol.", example = "R")
		String symbol,

		@Schema(description = "Formatted display amount.", example = "R20.50")
		String formatted
) {
}
