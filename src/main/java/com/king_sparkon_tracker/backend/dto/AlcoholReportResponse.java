package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record AlcoholReportResponse(
		@Schema(description = "Report start date/time.")
		LocalDateTime from,
		@Schema(description = "Report end date/time.")
		LocalDateTime to,
		@Schema(description = "Alcohol units bought in the range.", example = "10")
		int boughtQuantity,
		@Schema(description = "Alcohol units sold in the range.", example = "5")
		int soldQuantity,
		@Schema(description = "Total value of bought alcohol.", example = "150.00")
		BigDecimal boughtValue,
		@Schema(description = "Total value of sold alcohol.", example = "102.00")
		BigDecimal soldValue) {
}
