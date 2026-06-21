package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error envelope returned by validation, authentication, and domain exceptions.")
public record ApiError(
		@Schema(description = "Server time when the error was generated.", example = "2026-06-01T14:30:00")
		LocalDateTime timestamp,
		@Schema(description = "HTTP status code.", example = "400")
		int status,
		@Schema(description = "HTTP reason phrase.", example = "Bad Request")
		String error,
		@Schema(description = "Safe client-facing explanation.", example = "Product not found: 42")
		String message) {

	/**
	 * Builds a consistent error envelope for every exception handler.
	 */
	public static ApiError of(int status, String error, String message) {
		return new ApiError(LocalDateTime.now(), status, error, message);
	}
}
