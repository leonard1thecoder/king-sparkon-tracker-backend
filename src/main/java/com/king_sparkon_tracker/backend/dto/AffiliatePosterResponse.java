package com.king_sparkon_tracker.backend.dto;

import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.AffiliatePoster;
import com.king_sparkon_tracker.backend.model.AffiliatePosterCategory;

public record AffiliatePosterResponse(
		Long id,
		AffiliatePosterCategory category,
		String title,
		String description,
		String imageUrl,
		boolean active,
		String createdBy,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public static AffiliatePosterResponse from(AffiliatePoster poster) {
		return new AffiliatePosterResponse(
				poster.getId(),
				poster.getCategory(),
				poster.getTitle(),
				poster.getDescription(),
				poster.getImageUrl(),
				poster.isActive(),
				poster.getCreatedBy(),
				poster.getCreatedAt(),
				poster.getUpdatedAt());
	}
}
