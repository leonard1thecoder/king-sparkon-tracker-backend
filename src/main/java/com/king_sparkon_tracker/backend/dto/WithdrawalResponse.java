package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TipWithdrawalStatus;

public record WithdrawalResponse(
		Long id,
		Long workerId,
		Long ownerId,
		BigDecimal amount,
		MoneyResponse localizedAmount,
		int tipCount,
		String paypalEmail,
		TipWithdrawalStatus status,
		OffsetDateTime requestedAt,
		OffsetDateTime updated
) {

	public static WithdrawalResponse from(TipWithdrawal withdrawal, MoneyResponse localizedAmount) {
		return new WithdrawalResponse(
				withdrawal.getId(),
				withdrawal.getWorkerId(),
				withdrawal.getOwnerId(),
				withdrawal.getAmount(),
				localizedAmount,
				withdrawal.getTipCount(),
				withdrawal.getPaypalEmail(),
				withdrawal.getStatus(),
				withdrawal.getRequestedAt(),
				withdrawal.getUpdated()
		);
	}
}
