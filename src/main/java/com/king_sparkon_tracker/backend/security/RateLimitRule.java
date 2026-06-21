package com.king_sparkon_tracker.backend.security;

import com.king_sparkon_tracker.backend.model.BusinessPlan;

public record RateLimitRule(
		String label,
		BusinessPlan plan,
		int limit,
		long windowSeconds,
		long retryAfterSeconds) {
}
