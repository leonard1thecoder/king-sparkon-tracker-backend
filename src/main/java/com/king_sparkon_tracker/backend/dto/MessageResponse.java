package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

public record MessageResponse(
		String message,
		LocalDateTime timestamp
) {

	public static MessageResponse of(String message) {
		return new MessageResponse(message, LocalDateTime.now());
	}
}
