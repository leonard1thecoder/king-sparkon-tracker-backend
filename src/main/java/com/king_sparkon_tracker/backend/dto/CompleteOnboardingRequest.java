package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteOnboardingRequest(
		@Schema(description = "User physical address.", example = "12 Main Road, Johannesburg")
		@NotBlank
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "User cellphone number.", example = "+27821234567")
		@NotBlank
		@Size(max = 32)
		String cellphoneNumber
) {
}
