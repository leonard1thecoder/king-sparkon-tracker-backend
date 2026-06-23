package com.king_sparkon_tracker.backend.dto;

import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.PayoutAccountStatus;
import com.king_sparkon_tracker.backend.model.WorkerPayoutAccount;

public record PayPalAccountResponse(
		Long id,
		Long workerId,
		Long ownerId,
		String paypalEmail,
		PayoutAccountStatus status,
		String onboardingUrl,
		OffsetDateTime created,
		OffsetDateTime updated
) {

	public static PayPalAccountResponse from(WorkerPayoutAccount account) {
		return new PayPalAccountResponse(
				account.getId(),
				account.getWorkerId(),
				account.getOwnerId(),
				account.getPaypalEmail(),
				account.getStatus(),
				account.getOnboardingUrl(),
				account.getCreated(),
				account.getUpdated()
		);
	}
}
