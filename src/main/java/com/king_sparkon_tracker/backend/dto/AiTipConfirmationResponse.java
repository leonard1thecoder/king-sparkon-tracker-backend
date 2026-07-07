package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.TipStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiTipConfirmationResponse(
		@Schema(description = "Tip id when confirming one tip.", example = "42")
		Long tipId,

		@Schema(description = "Worker id tied to the tip confirmation.", example = "7")
		Long workerId,

		@Schema(description = "Number of tips in this confirmation.", example = "3")
		long tipCount,

		@Schema(description = "Paid tip count.", example = "2")
		long paidCount,

		@Schema(description = "Unpaid tip count.", example = "1")
		long unpaidCount,

		@Schema(description = "Tip status for single-tip confirmations.", example = "PAID")
		TipStatus status,

		@Schema(description = "Gross tip amount.", example = "120.00")
		BigDecimal grossAmount,

		@Schema(description = "Estimated withdrawal fee amount based on current tip withdrawal fee percent.", example = "10.20")
		BigDecimal estimatedWithdrawalFee,

		@Schema(description = "Estimated net amount after withdrawal fee.", example = "109.80")
		BigDecimal estimatedNetAmount,

		@Schema(description = "Withdrawal id if already assigned.", example = "5")
		Long withdrawalId,

		@Schema(description = "Whether the confirmed paid tips already belong to a withdrawal.", example = "false")
		boolean withdrawalAssigned,

		@Schema(description = "Creation date for single-tip confirmations.")
		OffsetDateTime created,

		@Schema(description = "Update date for single-tip confirmations.")
		OffsetDateTime updated,

		@Schema(description = "System conclusion before AI explanation.")
		String conclusion,

		@Schema(description = "AI-generated read-only explanation.")
		String aiExplanation
) {
}
