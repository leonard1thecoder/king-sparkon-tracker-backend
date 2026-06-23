package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionWithdrawalEligibilityResponse(
		Long ownerId,
		Long businessId,
		BigDecimal grossAmount,
		MoneyResponse localizedGrossAmount,
		BigDecimal feeAmount,
		MoneyResponse localizedFeeAmount,
		BigDecimal feePercent,
		BigDecimal availableAmount,
		MoneyResponse localizedAvailableAmount,
		MoneyResponse localizedMinimumAmount,
		int eligibleTransactionCount,
		int holdDays,
		boolean paypalAccountReady,
		boolean canWithdraw,
		LocalDateTime availableBefore
) {
}
