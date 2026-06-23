package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.AffiliateCommission;
import com.king_sparkon_tracker.backend.model.AffiliateCommissionStatus;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

public record AffiliateCommissionResponse(
		Long id,
		Long businessId,
		String businessName,
		Long subscriptionId,
		BusinessPlan businessPlan,
		BigDecimal grossAmount,
		BigDecimal commissionRatePercent,
		BigDecimal commissionAmount,
		String currency,
		AffiliateCommissionStatus status,
		Long withdrawalId,
		LocalDateTime earnedAt
) {

	public static AffiliateCommissionResponse from(AffiliateCommission commission) {
		return new AffiliateCommissionResponse(
				commission.getId(),
				commission.getBusinessId(),
				commission.getBusiness().getName(),
				commission.getSubscriptionId(),
				commission.getSubscription().getBusinessPlan(),
				commission.getGrossAmount(),
				commission.getCommissionRatePercent(),
				commission.getCommissionAmount(),
				commission.getCurrency(),
				commission.getStatus(),
				commission.getWithdrawalId(),
				commission.getEarnedAt());
	}
}
