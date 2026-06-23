package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TipRequest(
		@NotNull
		Long workerId,

		@NotNull
		@DecimalMin(value = "0.01")
		@Digits(integer = 10, fraction = 2)
		BigDecimal tipAmount,

		@Size(max = 2048)
		String callbackUrl
) {
}
