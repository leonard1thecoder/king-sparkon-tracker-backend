package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.Promotion;
import com.king_sparkon_tracker.backend.model.PromotionAudience;
import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.PromotionOrigin;
import com.king_sparkon_tracker.backend.model.PromotionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record PromotionResponse(
		@Schema(description = "Promotion id.", example = "1")
		Long id,

		@Schema(description = "Business id for owner-created promotions.", example = "3")
		Long businessId,

		@Schema(description = "Promotion title.", example = "Weekend special")
		String title,

		@Schema(description = "Promotion message.")
		String message,

		@Schema(description = "Landing URL.")
		String landingUrl,

		@Schema(description = "Promotion origin.", example = "OWNER")
		PromotionOrigin origin,

		@Schema(description = "Promotion channel.", example = "ANY")
		PromotionChannel channel,

		@Schema(description = "Promotion audience.", example = "REGISTERED_AFFILIATES")
		PromotionAudience audience,

		@Schema(description = "Promotion status.", example = "ACTIVE")
		PromotionStatus status,

		@Schema(description = "Number of active subscribers targeted when promotion was created.", example = "250")
		int targetCount,

		@Schema(description = "Bulk campaign price.", example = "211.50")
		BigDecimal bulkPrice,

		@Schema(description = "Promotion creator username.", example = "owner")
		String createdBy,

		@Schema(description = "Scheduled send time.")
		OffsetDateTime scheduledFor,

		@Schema(description = "Promotion expiry time.")
		OffsetDateTime expiresAt,

		@Schema(description = "Last scheduler processing time.")
		OffsetDateTime lastProcessedAt,

		@Schema(description = "Creation timestamp.")
		OffsetDateTime createdDate
) {

	public static PromotionResponse from(Promotion promotion) {
		return new PromotionResponse(
				promotion.getId(),
				promotion.getBusiness() == null ? null : promotion.getBusiness().getId(),
				promotion.getTitle(),
				promotion.getMessage(),
				promotion.getLandingUrl(),
				promotion.getOrigin(),
				promotion.getChannel(),
				promotion.getAudience(),
				promotion.getStatus(),
				promotion.getTargetCount(),
				promotion.getBulkPrice(),
				promotion.getCreatedBy(),
				promotion.getScheduledFor(),
				promotion.getExpiresAt(),
				promotion.getLastProcessedAt(),
				promotion.getCreatedDate()
		);
	}
}
