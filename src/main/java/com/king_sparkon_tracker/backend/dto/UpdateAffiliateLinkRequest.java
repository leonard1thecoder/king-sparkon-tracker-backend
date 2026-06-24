package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateAffiliateLinkRequest(
		String title,
		String description,
		String affiliateUrl,
		String imageUrl,
		String websiteName,
		String category,
		AffiliatePlacement placement,
		AffiliateLinkStatus status,
		@Min(value = 1, message = "Priority must be at least 1")
		@Max(value = 100, message = "Priority may not be greater than 100")
		Integer priority,
		List<BusinessPlan> displayPlans) {
}
