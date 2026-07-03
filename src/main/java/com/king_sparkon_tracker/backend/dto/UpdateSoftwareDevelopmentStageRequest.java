package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStage;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSoftwareDevelopmentStageRequest(
		@Schema(description = "Next Developer Hub delivery stage.", example = "DISCOVERY")
		@NotNull
		SoftwareDevelopmentStage stage,

		@Schema(description = "Optional status value. Defaults from the selected stage when not provided.", example = "IN_PROGRESS")
		SoftwareDevelopmentStatus status,

		@Schema(description = "Admin note shown to dashboards.", example = "Moved to Discovery from Admin Developer Hub.")
		@Size(max = 2000)
		String adminNote
) {
}
