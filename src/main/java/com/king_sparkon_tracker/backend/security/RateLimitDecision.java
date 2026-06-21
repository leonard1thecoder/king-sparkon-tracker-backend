package com.king_sparkon_tracker.backend.security;

public record RateLimitDecision(
		boolean allowed,
		String label,
		int limit,
		int remaining,
		long retryAfterSeconds,
		long resetAfterSeconds) {
}
