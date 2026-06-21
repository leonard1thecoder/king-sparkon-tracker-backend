package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.AuditLog;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuditLogResponse(
		@Schema(description = "Audit log id.", example = "1")
		Long id,
		@Schema(description = "Business id this audit log belongs to.", example = "3")
		Long businessId,
		@Schema(description = "Business name this audit log belongs to.", example = "Owner Retail Store")
		String businessName,
		@Schema(description = "Business action that occurred.", example = "TRANSACTION_CREATED")
		String action,
		@Schema(description = "Entity type affected by the action.", example = "InventoryTransaction")
		String entityType,
		@Schema(description = "Affected entity id.", example = "7")
		String entityId,
		@Schema(description = "Username that performed the action.", example = "owner")
		String actorUsername,
		@Schema(description = "Human-readable event details.")
		String details,
		@Schema(description = "Event creation timestamp.")
		LocalDateTime createdAt) {

	/**
	 * Converts the persistence model into an API-safe audit response.
	 */
	public static AuditLogResponse from(AuditLog auditLog) {
		return new AuditLogResponse(
				auditLog.getId(),
				auditLog.getBusiness() == null ? null : auditLog.getBusiness().getId(),
				auditLog.getBusiness() == null ? null : auditLog.getBusiness().getName(),
				auditLog.getAction(),
				auditLog.getEntityType(),
				auditLog.getEntityId(),
				auditLog.getActorUsername(),
				auditLog.getDetails(),
				auditLog.getCreatedAt());
	}
}
