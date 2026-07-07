package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BarcodeImageDecodeResponse(
		@Schema(description = "Decoded barcode or stock-unit value.", example = "5449000000996")
		String decodedValue,

		@Schema(description = "Decoder implementation used.", example = "ZXING")
		String decoder,

		@Schema(description = "Barcode format reported by the decoder.", example = "EAN_13")
		String format,

		@Schema(description = "Human-readable decode status.")
		String message
) {
}
