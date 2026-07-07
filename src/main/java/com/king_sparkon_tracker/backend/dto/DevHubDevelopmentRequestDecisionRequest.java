package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record DevHubDevelopmentRequestDecisionRequest(
		@Size(max = 1000, message = "reason must not exceed 1000 characters")
		@Schema(example = "Accepted after client confirmed the scope and budget.")
		String reason
) {
}
