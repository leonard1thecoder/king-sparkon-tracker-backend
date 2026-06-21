package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.king_sparkon_tracker.backend.model.ProductCategory;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductMovementResponse(
		@Schema(description = "Product id.", example = "9")
		Long productId,
		@Schema(description = "Product name.", example = "Beer")
		String productName,
		@Schema(description = "Product category.", example = "Alcohol")
		ProductCategory category,
		@Schema(description = "Assigned physical item barcodes for this product.")
		List<String> barcodes,
		@Schema(description = "Bought quantity in the report range.", example = "10")
		int boughtQuantity,
		@Schema(description = "Sold quantity in the report range.", example = "5")
		int soldQuantity,
		@Schema(description = "Bought value in the report range.", example = "150.00")
		BigDecimal boughtValue,
		@Schema(description = "Sold value in the report range.", example = "102.00")
		BigDecimal soldValue) {
}
