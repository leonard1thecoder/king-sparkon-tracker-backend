package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductPromotionResponse(
		Long id,
		Long productId,
		Long businessId,
		String businessName,
		BigDecimal promotionPrice,
		Long businessAccountEntryId,
		OffsetDateTime startsAt,
		OffsetDateTime endsAt,
		boolean active,
		OffsetDateTime createdAt,
		ProductResponse product
) {
}
