package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
		@Schema(description = "Registered username.", example = "owner") @NotBlank String username,
		@Schema(description = "Account password.", example = "secret") @NotBlank String password) {
}
