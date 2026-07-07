package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.DevHubDevelopmentRequest;
import com.king_sparkon_tracker.backend.model.DevHubRequestStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record DevHubDevelopmentRequestResponse(
		Long id,
		String clientName,
		String emailAddress,
		String phoneNumber,
		String companyName,
		String projectType,
		String title,
		String description,
		String budgetRange,
		String timeline,
		String currency,
		@Schema(example = "15000.00")
		BigDecimal estimatedMinPrice,
		@Schema(example = "35000.00")
		BigDecimal estimatedMaxPrice,
		String aiDevelopmentPlan,
		String aiAutomatedResponse,
		String decisionReason,
		DevHubRequestStatus status,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public static DevHubDevelopmentRequestResponse from(DevHubDevelopmentRequest request) {
		return new DevHubDevelopmentRequestResponse(
				request.getId(),
				request.getClientName(),
				request.getEmailAddress(),
				request.getPhoneNumber(),
				request.getCompanyName(),
				request.getProjectType(),
				request.getTitle(),
				request.getDescription(),
				request.getBudgetRange(),
				request.getTimeline(),
				request.getCurrency(),
				request.getEstimatedMinPrice(),
				request.getEstimatedMaxPrice(),
				request.getAiDevelopmentPlan(),
				request.getAiAutomatedResponse(),
				request.getDecisionReason(),
				request.getStatus(),
				request.getCreatedAt(),
				request.getUpdatedAt());
	}
}
