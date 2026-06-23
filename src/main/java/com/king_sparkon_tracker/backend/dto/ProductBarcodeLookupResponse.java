package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductBarcodeLookupResponse(
		@Schema(description = "Barcode id.", example = "14")
		Long id,
	@Schema(description = "Unique physical item barcode.", example = "6001")
	String barcode,
	@Schema(description = "Customer email reference for returnable claims.", example = "customer@example.com")
	String referenceEmail,
		@Schema(description = "Barcode claim status.", example = "NOT_CLAIMED")
		ProductBarcodeStatus status,
		@Schema(description = "Linked product id.", example = "9")
		Long productId,
		@Schema(description = "Linked product name.", example = "Barcode item")
		String productName,
		@Schema(description = "Linked product category.", example = "Alcohol")
		ProductCategory productCategory,
		@Schema(description = "Whether the linked product carries a returnable packaging claim.", example = "true")
		boolean returnableEnabled,
		@Schema(description = "Backward-compatible alias. Use returnableEnabled in new clients.", example = "true")
		boolean bottleReturnable) {

	public static ProductBarcodeLookupResponse from(ProductBarcode barcode) {
		return new ProductBarcodeLookupResponse(
				barcode.getId(),
				barcode.getBarcode(),
				barcode.getReferenceEmail(),
				barcode.getStatus(),
				barcode.getProduct().getId(),
				barcode.getProduct().getName(),
				barcode.getProduct().getCategory(),
				barcode.getProduct().isReturnableEnabled(),
				barcode.getProduct().isReturnableEnabled());
	}
}
