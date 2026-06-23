package com.king_sparkon_tracker.backend.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

final class EmailAddressNormalizer {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private EmailAddressNormalizer() {
	}

	static String normalizeOptional(String value, String message) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return normalize(value, message);
	}

	static String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return normalize(value, message);
	}

	private static String normalize(String value, String message) {
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException(message);
		}
		return normalized;
	}
}
