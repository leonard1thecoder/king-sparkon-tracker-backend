package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignTuckShopBarcodeRequest(
		@Schema(description = "Scanned stock-unit code or reusable product barcode.")
		@NotBlank
		@Size(max = 128)
		String barcode
) {
}
