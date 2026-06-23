package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.NotNull;

public record WithdrawalRequest(
		@NotNull
		Long workerId
) {
}
