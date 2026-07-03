package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.LocalizationCountry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAffiliateRequest(
		@Schema(description = "Affiliate login username.", example = "affiliate")
		@NotBlank
		String username,

		@Schema(description = "Affiliate email address.", example = "affiliate@example.com")
		@NotBlank
		@Email
		String emailAddress,

		@Schema(description = "Affiliate password.", example = "secret")
		@NotBlank
		String password,

		@Schema(description = "Affiliate localization country.", example = "SOUTH_AFRICA")
		LocalizationCountry localizationCountry,

		@Schema(description = "Affiliate physical address used for onboarding.", example = "12 Main Road, Johannesburg")
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "Affiliate cellphone number used for onboarding and payout contact.", example = "+27821234567")
		@NotBlank
		@Size(max = 32)
		String cellphoneNumber,

		@Schema(description = "Affiliate PayPal payout link or PayPal.me URL.", example = "https://paypal.me/affiliate")
		@Size(max = 2048)
		String paypalLink
) {
}
