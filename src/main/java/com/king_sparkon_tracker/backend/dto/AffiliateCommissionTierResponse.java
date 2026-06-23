package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;

public record AffiliateCommissionTierResponse(
		String label,
		int fromMonthInclusive,
		Integer toMonthExclusive,
		BigDecimal commissionRatePercent
) {
}
