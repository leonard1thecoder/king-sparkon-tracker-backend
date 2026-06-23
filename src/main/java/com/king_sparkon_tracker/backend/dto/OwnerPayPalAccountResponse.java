package com.king_sparkon_tracker.backend.dto;

import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.OwnerPayoutAccount;
import com.king_sparkon_tracker.backend.model.PayoutAccountStatus;

public record OwnerPayPalAccountResponse(
		Long id,
		Long ownerId,
		Long businessId,
		String paypalEmail,
		PayoutAccountStatus status,
		String onboardingUrl,
		OffsetDateTime created,
		OffsetDateTime updated
) {

	public static OwnerPayPalAccountResponse from(OwnerPayoutAccount account) {
		return new OwnerPayPalAccountResponse(
				account.getId(),
				account.getOwnerId(),
				account.getBusinessId(),
				account.getPaypalEmail(),
				account.getStatus(),
				account.getOnboardingUrl(),
				account.getCreated(),
				account.getUpdated()
		);
	}
}
