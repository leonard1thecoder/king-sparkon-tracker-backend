package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

public record InventorySummaryResponse(
		@Schema(description = "Total number of products.", example = "2")
		long totalProducts,
		@Schema(description = "Number of alcohol products.", example = "1")
		long alcoholProducts,
		@Schema(description = "Number of non-alcohol products.", example = "1")
		long nonAlcoholProducts,
		@Schema(description = "Total stock units across all products.", example = "15")
		int totalStockQuantity,
		@Schema(description = "Current stock value across all products.", example = "250.00")
		BigDecimal totalStockValue,
		@Schema(description = "Products at or below the low-stock threshold.", example = "1")
		long lowStockProducts) {
}
