package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TuckShopPurchaseItemRequest(
		@Schema(description = "Existing product id to purchase.", example = "9")
		@NotNull
		Long productId,

		@Schema(description = "Quantity to purchase.", example = "2")
		@NotNull
		@Positive
		Integer quantity,

		@Schema(description = "Worker-scanned product barcodes. Required for worker barcode checkout, optional for self-service checkout.")
		List<String> barcodes
) {
}
