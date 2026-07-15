package com.king_sparkon_tracker.backend.outbox;

import java.time.LocalDateTime;
import java.util.Map;

public final class OutboxPayloads {
	private OutboxPayloads() {
	}

	public record Email(String to, String subject, String html, String eventName) {
	}

	public record Notification(String eventName, Map<String, Object> attributes) {
	}

	public record QrGeneration(String aggregateType, String aggregateId, String value) {
	}

	public record AffiliateCalculation(Long businessSubscriptionId, Long businessId, LocalDateTime earnedAt) {
	}

	public record ReportGeneration(Long businessId, String reportType) {
	}
}
