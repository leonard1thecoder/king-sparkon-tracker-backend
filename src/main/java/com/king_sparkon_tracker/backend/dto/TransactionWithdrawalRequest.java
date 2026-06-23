package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record TransactionWithdrawalRequest(
		@Size(max = 500)
		@Schema(description = "Optional website-payment transaction ids to withdraw. Omit to withdraw all eligible transactions.")
		List<Long> transactionIds
) {
}
