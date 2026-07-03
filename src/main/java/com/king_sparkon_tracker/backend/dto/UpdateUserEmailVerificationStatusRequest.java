package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateUserEmailVerificationStatusRequest(
		@Schema(
				description = "Set true to bypass email verification by marking the user email as verified. Set false to require verification again.",
				example = "true")
		@NotNull
		Boolean emailVerified
) {
}
