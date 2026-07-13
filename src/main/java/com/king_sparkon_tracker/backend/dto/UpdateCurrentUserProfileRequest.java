package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserProfileRequest(
		@Schema(description = "Updated account email address.", example = "worker@example.com")
		@NotBlank
		@Email
		@Size(max = 255)
		String emailAddress,

		@Schema(description = "User physical address.", example = "12 Main Road, Johannesburg")
		@NotBlank
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "User cellphone number.", example = "+27821234567")
		@NotBlank
		@Size(max = 32)
		String cellphoneNumber,

		@Schema(description = "Optional profile-picture URL.", example = "https://cdn.example.com/profiles/worker.png")
		@Size(max = 2048)
		String profilePictureUrl) {
}
