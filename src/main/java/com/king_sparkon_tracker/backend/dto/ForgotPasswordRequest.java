package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
		@NotBlank(message = "Email address is required")
		@Email(message = "Email address must be valid")
		@Size(max = 255, message = "Email address must not exceed 255 characters")
		String emailAddress
) {
}
