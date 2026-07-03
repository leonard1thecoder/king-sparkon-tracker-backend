package com.king_sparkon_tracker.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@Schema(description = "Login username.", example = "owner")
		@NotBlank
		String username,

		@Schema(description = "Email address.", example = "owner@example.com")
		@NotBlank
		@Email
		String emailAddress,

		@Schema(description = "Account password.", example = "StrongPassword123")
		@NotBlank
		String password,

		@Schema(description = "Business name this owner is registering. Required when serviceRegisteringFor is BUSINESS_OWNER.", example = "Owner Retail Store")
		@Size(max = 255)
		String businessName,

		@Schema(description = "Business description captured during owner onboarding.", example = "Barcode-enabled retail store selling beverages and convenience products.")
		@Size(max = 2000)
		String businessDescription,

		@JsonAlias({ "LocalizationContry", "localizationContry" })
		@Schema(description = "User localization country.", example = "SOUTH_AFRICA")
		LocalizationCountry localizationCountry,

		@Schema(description = "Physical address used for onboarding.", example = "12 Main Road, Johannesburg")
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "Cellphone number used for onboarding and account contact.", example = "+27821234567")
		@NotBlank
		@Size(max = 32)
		String cellphoneNumber,

		@JsonAlias({ "referenceCode", "referralCode", "promoCode" })
		@Schema(description = "Optional affiliate, reference, referral, or promo code from a pricing-page promotion QR.", example = "AFF-ALICE-1234")
		@Size(max = 64)
		String affiliateCode,

		@Schema(description = "What this registration is for. This maps to the backend privilege.", example = "BUSINESS_OWNER")
		ServiceRegistrationFor serviceRegisteringFor,

		@Schema(description = "Affiliate PayPal payout link or PayPal.me URL. Used only for AFFILIATE registration.", example = "https://paypal.me/affiliate")
		@Size(max = 2048)
		String paypalLink
) {
	public RegisterUserRequest {
		if (serviceRegisteringFor == null) {
			serviceRegisteringFor = ServiceRegistrationFor.BUSINESS_OWNER;
		}
	}

	public RegisterUserRequest(
			String username,
			String emailAddress,
			String password,
			String businessName,
			String businessDescription,
			LocalizationCountry localizationCountry,
			String physicalAddress,
			String cellphoneNumber,
			String affiliateCode) {
		this(
				username,
				emailAddress,
				password,
				businessName,
				businessDescription,
				localizationCountry,
				physicalAddress,
				cellphoneNumber,
				affiliateCode,
				ServiceRegistrationFor.BUSINESS_OWNER,
				null);
	}

	public RegisterUserRequest(
			String username,
			String emailAddress,
			String password,
			String businessName,
			LocalizationCountry localizationCountry) {
		this(username, emailAddress, password, businessName, null, localizationCountry, null, null, null, ServiceRegistrationFor.BUSINESS_OWNER, null);
	}

	public RegisterUserRequest(
			String username,
			String emailAddress,
			String password,
			String businessName,
			LocalizationCountry localizationCountry,
			String physicalAddress,
			String cellphoneNumber) {
		this(username, emailAddress, password, businessName, null, localizationCountry, physicalAddress, cellphoneNumber, null, ServiceRegistrationFor.BUSINESS_OWNER, null);
	}
}
