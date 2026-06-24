package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

public record PromotionPriceQuoteResponse(
		@Schema(description = "Target subscriber count.", example = "250")
		int targetCount,

		@Schema(description = "Fixed platform campaign fee.", example = "49.00")
		BigDecimal platformFee,

		@Schema(description = "Price per subscriber for this bulk tier.", example = "0.65")
		BigDecimal pricePerSubscriber,

		@Schema(description = "Total campaign price.", example = "211.50")
		BigDecimal totalPrice,

		@Schema(description = "Pricing tier label.", example = "101-1000")
		String tier
) {
}
