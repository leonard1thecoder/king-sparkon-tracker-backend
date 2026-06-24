package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.AffiliateLink;
import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

public record AffiliateLinkResponse(
		Long id,
		String title,
		String description,
		String affiliateUrl,
		String imageUrl,
		String websiteName,
		String category,
		AffiliatePlacement placement,
		AffiliateLinkStatus status,
		int priority,
		List<BusinessPlan> displayPlans,
		long impressionCount,
		long clickCount,
		LocalDateTime createdDate,
		LocalDateTime modifiedDate) {

	public static AffiliateLinkResponse from(AffiliateLink link) {
		return new AffiliateLinkResponse(
				link.getId(),
				link.getTitle(),
				link.getDescription(),
				link.getAffiliateUrl(),
				link.getImageUrl(),
				link.getWebsiteName(),
				link.getCategory(),
				link.getPlacement(),
				link.getStatus(),
				link.getPriority(),
				displayPlans(link),
				link.getImpressionCount(),
				link.getClickCount(),
				link.getCreatedDate(),
				link.getModifiedDate());
	}

	private static List<BusinessPlan> displayPlans(AffiliateLink link) {
		return java.util.Arrays.stream(BusinessPlan.values())
				.filter(link::supportsPlan)
				.toList();
	}
}
