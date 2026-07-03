package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSoftwareDevelopmentRequest(
		@Schema(description = "Name of the software the business owner wants King Sparkon Dev Hub to build.", example = "Worker QR tip payout portal")
		@NotBlank
		@Size(min = 3, max = 160)
		String softwareName,

		@Schema(description = "Detailed explanation of users, dashboards, payments, reports, QR/barcode flows, admin controls, integrations, and support.")
		@NotBlank
		@Size(min = 30, max = 4000)
		String softwareDescription,

		@Schema(description = "True when the owner wants cloud maintenance after delivery.", example = "true")
		boolean requiresCloudMaintenance,

		@Schema(description = "True when the owner wants QA regression coverage after delivery.", example = "true")
		boolean requiresQualityAssuranceRegression
) {
}
