package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.king_sparkon_tracker.backend.model.TransactionItem;

import io.swagger.v3.oas.annotations.media.Schema;

public record TuckShopPurchaseItemResponse(
		@Schema(description = "Existing product id.", example = "9")
		Long productId,

		@Schema(description = "Product name snapshot currently stored on the linked product.", example = "Coke 500ml")
		String productName,

		@Schema(description = "Product image URL shown in the tuck shop.")
		String productImageUrl,

		@Schema(description = "Quantity purchased.", example = "2")
		int quantity,

		@Schema(description = "Unit price captured by the transaction.", example = "20.50")
		BigDecimal unitPrice,

		@Schema(description = "Line total.", example = "41.00")
		BigDecimal lineTotal,

		@Schema(description = "Barcodes attached to the sale transaction.")
		List<String> barcodes
) {
	public static TuckShopPurchaseItemResponse from(TransactionItem item) {
		return new TuckShopPurchaseItemResponse(
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getProduct().getProductImageUrl(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
				item.getBarcodes()
		);
	}
}
