package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerPayPalAccountOnboardingRequest(
		@NotBlank
		@Email
		@Size(max = 255)
		@Schema(description = "Owner PayPal email for website-payment transaction withdrawals.", example = "owner@paypal.com")
		String paypalEmail,

		@Size(max = 2048)
		@Schema(description = "Optional owner-dashboard callback URL.", example = "https://app.example/dashboard/owner/transactions/paypal")
		String callbackUrl
) {
}
