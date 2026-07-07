package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductBarcodeResponse(
		@Schema(description = "Stock unit id.", example = "14")
		Long id,

		@Schema(description = "Unique King Sparkon stock-unit code.", example = "KST-UNIT-000001")
		String unitCode,

		@Schema(description = "Reusable retail product barcode snapshot for this unit.", example = "5449000000996")
		String barcode,

		@Schema(description = "Optional customer email reference for returnable claims.", example = "customer@example.com")
		String referenceEmail,

		@Schema(description = "Stock unit claim status.", example = "NOT_CLAIMED")
		ProductBarcodeStatus status
) {

	public ProductBarcodeResponse(Long id, String barcode, String referenceEmail, ProductBarcodeStatus status) {
		this(id, barcode, barcode, referenceEmail, status);
	}

	public static ProductBarcodeResponse from(ProductBarcode barcode) {
		return new ProductBarcodeResponse(
				barcode.getId(),
				barcode.getUnitCode(),
				barcode.getBarcode(),
				barcode.getReferenceEmail(),
				barcode.getStatus()
		);
	}
}
