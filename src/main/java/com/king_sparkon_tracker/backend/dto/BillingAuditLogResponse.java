package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.BillingAuditLog;

public record BillingAuditLogResponse(
		Long id,
		Long businessId,
		String businessName,
		BillingAuditAction action,
		String actorUsername,
		String paypalEventId,
		String paypalSubscriptionId,
		String message,
		LocalDateTime createdDate
) {

	public static BillingAuditLogResponse from(BillingAuditLog log) {
		return new BillingAuditLogResponse(
				log.getId(),
				log.getBusiness() == null ? null : log.getBusiness().getId(),
				log.getBusiness() == null ? null : log.getBusiness().getName(),
				log.getAction(),
				log.getActorUsername(),
				log.getPaypalEventId(),
				log.getPaypalSubscriptionId(),
				log.getMessage(),
				log.getCreatedDate()
		);
	}
}
