package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
		@NotBlank(message = "Refresh token is required")
		@Schema(description = "Opaque refresh token returned during login.")
		String refreshToken) {
}
