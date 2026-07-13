package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserPasswordRequest(
		@Schema(description = "Current account password.")
		@NotBlank
		String currentPassword,

		@Schema(description = "New account password.")
		@NotBlank
		@Size(min = 8, max = 120)
		String newPassword,

		@Schema(description = "Confirmation of the new account password.")
		@NotBlank
		String confirmPassword) {
}
