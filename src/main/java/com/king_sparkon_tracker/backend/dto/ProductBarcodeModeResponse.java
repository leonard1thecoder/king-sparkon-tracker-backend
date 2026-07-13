package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcodeMode;

public record ProductBarcodeModeResponse(
		Long productId,
		String productName,
		ProductBarcodeMode barcodeMode,
		String manufacturerBarcode,
		int stockQuantity,
		int barcodeCount,
		int barcodesRequired
) {
	public static ProductBarcodeModeResponse from(Product product, ProductBarcodeMode mode) {
		int count = product.getBarcodes() == null ? 0 : product.getBarcodes().size();
		return new ProductBarcodeModeResponse(
				product.getId(),
				product.getName(),
				mode,
				product.getProductBarcode(),
				product.getStockQuantity(),
				count,
				Math.max(product.getStockQuantity() - count, 0));
	}
}
