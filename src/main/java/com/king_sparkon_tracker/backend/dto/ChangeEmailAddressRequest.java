package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailAddressRequest(
		@Schema(description = "New email address for the authenticated account.", example = "new-email@example.com")
		@NotBlank
		@Email
		@Size(max = 255)
		String emailAddress
) {
}
