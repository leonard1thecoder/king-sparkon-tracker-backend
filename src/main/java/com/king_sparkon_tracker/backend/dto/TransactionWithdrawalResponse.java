package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawalStatus;

public record TransactionWithdrawalResponse(
		Long id,
		Long ownerId,
		Long businessId,
		BigDecimal grossAmount,
		MoneyResponse localizedGrossAmount,
		BigDecimal feeAmount,
		MoneyResponse localizedFeeAmount,
		BigDecimal feePercent,
		BigDecimal amount,
		MoneyResponse localizedAmount,
		int transactionCount,
		String paypalEmail,
		TransactionWithdrawalStatus status,
		OffsetDateTime requestedAt,
		OffsetDateTime updated
) {

	public static TransactionWithdrawalResponse from(
			TransactionWithdrawal withdrawal,
			MoneyResponse localizedGrossAmount,
			MoneyResponse localizedFeeAmount,
			MoneyResponse localizedAmount) {
		return new TransactionWithdrawalResponse(
				withdrawal.getId(),
				withdrawal.getOwnerId(),
				withdrawal.getBusinessId(),
				withdrawal.getGrossAmount(),
				localizedGrossAmount,
				withdrawal.getFeeAmount(),
				localizedFeeAmount,
				withdrawal.getFeePercent(),
				withdrawal.getAmount(),
				localizedAmount,
				withdrawal.getTransactionCount(),
				withdrawal.getPaypalEmail(),
				withdrawal.getStatus(),
				withdrawal.getRequestedAt(),
				withdrawal.getUpdated()
		);
	}
}
