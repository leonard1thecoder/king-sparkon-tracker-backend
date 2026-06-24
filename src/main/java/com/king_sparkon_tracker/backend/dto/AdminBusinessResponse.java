package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminBusinessResponse(
		@Schema(description = "Business id.", example = "3")
		Long id,

		@Schema(description = "Business name.", example = "Owner Retail Store")
		String name,

		@Schema(description = "Owner user id.", example = "12")
		Long ownerId,

		@Schema(description = "Owner username.", example = "owner")
		String ownerUsername,

		@Schema(description = "Owner email address.", example = "owner@example.com")
		String ownerEmailAddress,

		@Schema(description = "Current business plan.", example = "FREE_TRIAL")
		BusinessPlan businessPlan,

		@Schema(description = "Current business status.", example = "TRIAL")
		BusinessStatus businessStatus,

		@Schema(description = "Current billing period end date.")
		LocalDateTime currentBillingPeriodEndDate,

		@Schema(description = "Affiliate code attached to this business when present.")
		String affiliateCode,

		@Schema(description = "Business creation timestamp.")
		LocalDateTime createdDate,

		@Schema(description = "Business last modification timestamp.")
		LocalDateTime modifiedDate
) {

	public static AdminBusinessResponse from(Business business) {
		return new AdminBusinessResponse(
				business.getId(),
				business.getName(),
				business.getOwner() == null ? null : business.getOwner().getId(),
				business.getOwner() == null ? null : business.getOwner().getUsername(),
				business.getOwner() == null ? null : business.getOwner().getEmailAddress(),
				business.getBusinessPlan(),
				business.getBusinessStatus(),
				business.getCurrentBillingPeriodEndDate(),
				business.getAffiliateCode(),
				business.getCreatedDate(),
				business.getModifiedDate()
		);
	}
}
