package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.AffiliateWithdrawal;
import com.king_sparkon_tracker.backend.model.AffiliateWithdrawalStatus;

public record AffiliateWithdrawalResponse(
		Long id,
		Long affiliateId,
		BigDecimal amount,
		MoneyResponse amountLocalized,
		String currency,
		int commissionCount,
		String paypalLink,
		AffiliateWithdrawalStatus status,
		LocalDateTime requestedAt,
		LocalDateTime updated
) {

	public static AffiliateWithdrawalResponse from(AffiliateWithdrawal withdrawal, MoneyResponse amountLocalized) {
		return new AffiliateWithdrawalResponse(
				withdrawal.getId(),
				withdrawal.getAffiliateId(),
				withdrawal.getAmount(),
				amountLocalized,
				withdrawal.getCurrency(),
				withdrawal.getCommissionCount(),
				withdrawal.getPaypalLink(),
				withdrawal.getStatus(),
				withdrawal.getRequestedAt(),
				withdrawal.getUpdated());
	}
}
