package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductQuantityRequest(
		@Schema(description = "Updated product stock quantity.", example = "20") @NotNull @PositiveOrZero Integer stockQuantity) {
}
