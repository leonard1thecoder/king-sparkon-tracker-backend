package com.king_sparkon_tracker.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateWorkerRequest(
		@Schema(description = "Worker login username.", example = "worker") @NotBlank String username,
		@Schema(description = "Worker email address.", example = "worker@example.com") @NotBlank @Email String emailAddress,
		@Schema(description = "Worker password.", example = "secret") @NotBlank String password) {
}
