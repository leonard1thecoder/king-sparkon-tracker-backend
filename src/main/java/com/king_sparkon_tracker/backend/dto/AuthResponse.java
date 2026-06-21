package com.king_sparkon_tracker.backend.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
		@Schema(description = "Token type.", example = "Bearer")
		String tokenType,
		@Schema(description = "JWT access token.")
		String accessToken,
		@Schema(description = "Token expiration timestamp.")
		Instant expiresAt,
		@Schema(description = "Authenticated user profile.")
		UserResponse user) {
}
