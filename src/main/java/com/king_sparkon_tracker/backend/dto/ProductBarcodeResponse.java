package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductBarcodeResponse(
		@Schema(description = "Barcode id.", example = "14")
		Long id,

		@Schema(description = "Unique physical item barcode.", example = "6001")
		String barcode,

		@Schema(description = "Optional customer reference for returnable claims.", example = "0821234567")
		String referencee,

		@Schema(description = "Barcode claim status.", example = "NOT_CLAIMED")
		ProductBarcodeStatus status
) {

	public static ProductBarcodeResponse from(ProductBarcode barcode) {
		return new ProductBarcodeResponse(
				barcode.getId(),
				barcode.getBarcode(),
				barcode.getReferencee(),
				barcode.getStatus()
		);
	}
}
