package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record BarcodeVerificationResponse(
		@Schema(description = "Original scanned value.", example = "5449000000996")
		String input,

		@Schema(description = "Normalized scanned value used for lookup.", example = "5449000000996")
		String normalizedValue,

		@Schema(description = "Decoder used to extract the value.", example = "BROWSER_BARCODE_DETECTOR")
		String decoder,

		@Schema(description = "Whether the value matched a product barcode, stock unit, or nothing.", example = "PRODUCT_BARCODE")
		BarcodeMatchType matchType,

		@Schema(description = "Verification status.", example = "FOUND")
		String status,

		@Schema(description = "Product id when found.", example = "12")
		Long productId,

		@Schema(description = "Product name when found.", example = "Coca-Cola 500ml")
		String productName,

		@Schema(description = "Reusable product barcode when available.", example = "5449000000996")
		String productBarcode,

		@Schema(description = "Current product stock quantity.", example = "100")
		Integer stockQuantity,

		@Schema(description = "Unique stock unit code when matchType is STOCK_UNIT.", example = "KST-UNIT-000001")
		String unitCode,

		@Schema(description = "Stock unit availability when matchType is STOCK_UNIT.", example = "AVAILABLE")
		ProductBarcodeAvailabilityStatus availabilityStatus,

		@Schema(description = "Returnable claim status when matchType is STOCK_UNIT.", example = "NOT_CLAIMED")
		ProductBarcodeStatus claimStatus,

		@Schema(description = "Human-readable verification result.")
		String message,

		@Schema(description = "AI-generated explanation or safe fallback explanation.")
		String aiExplanation
) {
}
