package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;

public record AffiliateWithdrawalEligibilityResponse(
		Long affiliateId,
		BigDecimal availableAmount,
		MoneyResponse availableAmountLocalized,
		int eligibleCommissionCount,
		boolean paypalLinkReady,
		boolean canWithdraw,
		String paypalLink
) {
}
