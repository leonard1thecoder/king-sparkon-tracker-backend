package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactInquiryRequest(
		@Size(max = 160)
		String contactName,

		@NotBlank
		@Size(max = 255)
		String businessName,

		@NotBlank
		@Email
		@Size(max = 255)
		String emailAddress,

		@Size(max = 64)
		String phoneNumber,

		@NotBlank
		@Size(max = 2000)
		String message
) {
}
