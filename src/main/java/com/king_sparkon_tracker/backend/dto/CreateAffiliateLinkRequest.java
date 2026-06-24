package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAffiliateLinkRequest(
		@NotBlank(message = "Title is required")
		String title,
		String description,
		@NotBlank(message = "Affiliate URL is required")
		String affiliateUrl,
		String imageUrl,
		@NotBlank(message = "Website name is required")
		String websiteName,
		String category,
		@NotNull(message = "Placement is required")
		AffiliatePlacement placement,
		AffiliateLinkStatus status,
		@Min(value = 1, message = "Priority must be at least 1")
		@Max(value = 100, message = "Priority may not be greater than 100")
		Integer priority,
		List<BusinessPlan> displayPlans) {
}
