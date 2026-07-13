package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.ProductBarcodeMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductBarcodeModeRequest(
		@NotNull ProductBarcodeMode barcodeMode,
		@Size(max = 128) String manufacturerBarcode
) {
}
