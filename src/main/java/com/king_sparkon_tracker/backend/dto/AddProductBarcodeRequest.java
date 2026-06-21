package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AddProductBarcodeRequest(
		@Schema(description = "Unique barcode for one physical stocked item.", example = "6001")
		@NotBlank
		String barcode,
		@Schema(description = "Optional customer reference for returnable claims, usually a cellphone number.", example = "0821234567")
		String referencee) {
}
