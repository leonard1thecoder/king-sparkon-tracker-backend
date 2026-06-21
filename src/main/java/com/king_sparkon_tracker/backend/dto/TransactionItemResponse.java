package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.king_sparkon_tracker.backend.model.TransactionItem;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransactionItemResponse(
		@Schema(description = "Transaction item id.", example = "1")
		Long id,
		@Schema(description = "Product id.", example = "9")
		Long productId,
		@Schema(description = "Product name.", example = "Beer")
		String productName,
		@Schema(description = "Quantity moved.", example = "2")
		int quantity,
		@Schema(description = "Unit price used for this line item.", example = "20.50")
		BigDecimal unitPrice,
		@Schema(description = "Item barcodes attached to this transaction item.")
		List<String> barcodes) {

	/**
	 * Converts a transaction item entity into a response enriched with product details.
	 */
	public static TransactionItemResponse from(TransactionItem item) {
		return new TransactionItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getQuantity(),
				item.getUnitPrice(),
				List.copyOf(item.getBarcodes()));
	}
}
