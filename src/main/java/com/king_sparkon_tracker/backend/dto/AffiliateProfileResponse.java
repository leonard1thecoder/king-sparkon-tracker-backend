package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.TrackerUser;

public record AffiliateProfileResponse(
		Long id,
		String username,
		String emailAddress,
		String physicalAddress,
		String cellphoneNumber,
		String profilePictureUrl,
		boolean onboardingRequired,
		boolean onboardingCompleted,
		String paypalLink,
		String affiliateCode,
		String promotionUrl,
		String qrCodeUrl,
		LocalDateTime affiliateJoinedAt
) {

	public static AffiliateProfileResponse from(TrackerUser affiliate) {
		return new AffiliateProfileResponse(
				affiliate.getId(),
				affiliate.getUsername(),
				affiliate.getEmailAddress(),
				affiliate.getPhysicalAddress(),
				affiliate.getCellphoneNumber(),
				affiliate.getProfilePictureUrl(),
				affiliate.isOnboardingRequired(),
				affiliate.isOnboardingCompleted(),
				affiliate.getAffiliatePaypalLink(),
				affiliate.getAffiliateCode(),
				affiliate.getAffiliatePromotionUrl(),
				affiliate.getAffiliateQrCodeUrl(),
				affiliate.getAffiliateJoinedAt());
	}
}
