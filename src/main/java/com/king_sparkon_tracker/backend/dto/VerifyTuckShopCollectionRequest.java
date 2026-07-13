package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyTuckShopCollectionRequest(
		@Schema(description = "Collection QR value shown by the worker.", example = "KST-COLLECT:1b54498b-2b69-4fce-a43c-d1190f43f920")
		@NotBlank
		@Size(max = 180)
		String qrValue
) {
}
