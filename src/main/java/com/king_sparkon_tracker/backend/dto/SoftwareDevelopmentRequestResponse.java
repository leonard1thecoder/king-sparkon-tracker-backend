package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.DeveloperHubSoftwareRequest;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStage;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record SoftwareDevelopmentRequestResponse(
		@Schema(description = "Request id.", example = "1")
		Long id,
		@Schema(description = "Business id.", example = "3")
		Long businessId,
		@Schema(description = "Business name.")
		String businessName,
		@Schema(description = "Owner id.", example = "7")
		Long ownerId,
		@Schema(description = "Owner username.")
		String ownerName,
		@Schema(description = "Owner email.")
		String ownerEmail,
		@Schema(description = "Software name.")
		String softwareName,
		@Schema(description = "Software description.")
		String softwareDescription,
		@Schema(description = "Cloud maintenance requested.", example = "true")
		boolean requiresCloudMaintenance,
		@Schema(description = "QA regression requested.", example = "true")
		boolean requiresQualityAssuranceRegression,
		@Schema(description = "Current stage.", example = "DISCOVERY")
		SoftwareDevelopmentStage stage,
		@Schema(description = "Current status.", example = "IN_PROGRESS")
		SoftwareDevelopmentStatus status,
		@Schema(description = "Latest admin note.")
		String adminNote,
		@Schema(description = "Request timestamp.")
		LocalDateTime requestedAt,
		@Schema(description = "Update timestamp.")
		LocalDateTime updatedAt,
		@Schema(description = "Start timestamp.")
		LocalDateTime startedAt,
		@Schema(description = "Quote timestamp.")
		LocalDateTime quoteSentAt
) {
	public static SoftwareDevelopmentRequestResponse from(DeveloperHubSoftwareRequest request) {
		return new SoftwareDevelopmentRequestResponse(
				request.getId(),
				request.getBusiness() == null ? null : request.getBusiness().getId(),
				request.getBusinessName(),
				request.getOwner() == null ? null : request.getOwner().getId(),
				request.getOwnerName(),
				request.getOwnerEmail(),
				request.getSoftwareName(),
				request.getSoftwareDescription(),
				request.isRequiresCloudMaintenance(),
				request.isRequiresQualityAssuranceRegression(),
				request.getStage(),
				request.getStatus(),
				request.getAdminNote(),
				request.getRequestedAt(),
				request.getUpdatedAt(),
				request.getStartedAt(),
				request.getQuoteSentAt()
		);
	}
}
