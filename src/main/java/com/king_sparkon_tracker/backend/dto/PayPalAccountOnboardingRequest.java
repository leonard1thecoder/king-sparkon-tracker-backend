package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PayPalAccountOnboardingRequest(
		@NotNull
		Long workerId,

		@NotBlank
		@Email
		@Size(max = 255)
		String paypalEmail,

		@Size(max = 2048)
		String callbackUrl
) {
}
