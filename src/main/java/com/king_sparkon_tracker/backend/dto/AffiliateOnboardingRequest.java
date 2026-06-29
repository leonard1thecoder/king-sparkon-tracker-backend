package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AffiliateOnboardingRequest(
		@Schema(description = "Affiliate physical address.", example = "12 Main Road, Johannesburg")
		@NotBlank
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "Affiliate cellphone number.", example = "+27821234567")
		@NotBlank
		@Size(max = 32)
		String cellphoneNumber,

		@Schema(description = "Affiliate payout link.")
		@NotBlank
		@Size(max = 2048)
		String paypalLink,

		@Schema(description = "Optional affiliate profile-picture URL.")
		@Size(max = 2048)
		String profilePictureUrl
) {
	public AffiliateOnboardingRequest(String physicalAddress, String cellphoneNumber, String paypalLink) {
		this(physicalAddress, cellphoneNumber, paypalLink, null);
	}
}
