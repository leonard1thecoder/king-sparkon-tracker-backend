package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WithdrawalEligibilityResponse(
		Long workerId,
		BigDecimal availableAmount,
		MoneyResponse localizedAvailableAmount,
		MoneyResponse localizedMinimumAmount,
		int eligibleTipCount,
		int holdDays,
		boolean paypalAccountReady,
		boolean canWithdraw,
		OffsetDateTime availableBefore
) {
}
