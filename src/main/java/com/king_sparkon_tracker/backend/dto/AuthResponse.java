package com.king_sparkon_tracker.backend.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
		@Schema(description = "Token type.", example = "Bearer")
		String tokenType,
		@Schema(description = "JWT access token.")
		String accessToken,
		@Schema(description = "Access token expiration timestamp.")
		Instant expiresAt,
		@Schema(description = "Opaque refresh token. Store securely and never log it.")
		String refreshToken,
		@Schema(description = "Refresh token expiration timestamp.")
		Instant refreshTokenExpiresAt,
		@Schema(description = "Authenticated user profile.")
		UserResponse user) {

	public AuthResponse(String tokenType, String accessToken, Instant expiresAt, UserResponse user) {
		this(tokenType, accessToken, expiresAt, null, null, user);
	}
}
