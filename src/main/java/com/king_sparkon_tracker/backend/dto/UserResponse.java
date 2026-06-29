package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
		@Schema(description = "User id.", example = "1")
		Long id,

		@Schema(description = "Login username.", example = "owner")
		String username,

		@Schema(description = "User email address.", example = "owner@example.com")
		String emailAddress,

		@Schema(description = "User creation timestamp.")
		LocalDateTime createdDate,

		@Schema(description = "Last modification timestamp.")
		LocalDateTime modifiedDate,

		@Schema(description = "User privilege role.", example = "Owner")
		PrivilegeRole privilege,

		@Schema(description = "Business id this user belongs to.", example = "3")
		Long businessId,

		@Schema(description = "Business name this user belongs to.", example = "Owner Retail Store")
		String businessName,

		@Schema(description = "QR code URL linked to the business public page or verification page.")
		String businessQrCodeUrl,

		@Schema(description = "User localization country.", example = "SOUTH_AFRICA")
		LocalizationCountry localizationCountry,

		@Schema(description = "User physical address captured during onboarding.")
		String physicalAddress,

		@Schema(description = "User cellphone number captured during onboarding.")
		String cellphoneNumber,

		@Schema(description = "Profile picture URL captured during onboarding or worker creation.")
		String profilePictureUrl,

		@Schema(description = "Worker job title.", example = "Cashier")
		String jobTitle,

		@Schema(description = "Whether the user still needs to complete first-login onboarding.", example = "true")
		boolean onboardingRequired,

		@Schema(description = "Whether onboarding details have been completed.", example = "false")
		boolean onboardingCompleted,

		@Schema(description = "Whether this worker has a static tip QR code enabled.", example = "true")
		boolean tipQrCodeEnabled,

		@Schema(description = "Static QR code URL linked to the worker tip payment flow.")
		String tipQrCodeUrl,

		@Schema(description = "Affiliate referral code for pricing-plan promotion.")
		String affiliateCode,

		@Schema(description = "Affiliate pricing-page promotion URL.")
		String affiliatePromotionUrl,

		@Schema(description = "Affiliate QR code URL linked to the promotion URL.")
		String affiliateQrCodeUrl,

		@Schema(description = "Affiliate PayPal payout link.")
		String affiliatePaypalLink
) {

	public static UserResponse from(TrackerUser user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmailAddress(),
				user.getCreatedDate(),
				user.getModifiedDate(),
				user.getPrivilege().getName(),
				user.getBusiness() == null ? null : user.getBusiness().getId(),
				user.getBusiness() == null ? null : user.getBusiness().getName(),
				user.getBusiness() == null ? null : user.getBusiness().getQrCodeUrl(),
				user.getLocalizationCountry(),
				user.getPhysicalAddress(),
				user.getCellphoneNumber(),
				user.getProfilePictureUrl(),
				user.getJobTitle(),
				user.isOnboardingRequired(),
				user.isOnboardingCompleted(),
				user.isTipQrCodeEnabled(),
				user.getTipQrCodeUrl(),
				user.getAffiliateCode(),
				user.getAffiliatePromotionUrl(),
				user.getAffiliateQrCodeUrl(),
				user.getAffiliatePaypalLink()
		);
	}
}
