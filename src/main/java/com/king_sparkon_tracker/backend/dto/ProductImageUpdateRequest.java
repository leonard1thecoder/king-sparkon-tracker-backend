package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageUpdateRequest(
		@Schema(description = "Product photo URL/path shown in King Sparkon Tuck Shop.", example = "https://storage.googleapis.com/king-sparkon/products/coke.png")
		@NotBlank
		@Size(max = 2048)
		String productImageUrl
) {
}
