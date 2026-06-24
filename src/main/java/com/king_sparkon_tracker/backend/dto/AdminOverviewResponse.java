package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminOverviewResponse(
		@Schema(description = "Total number of users on the platform.", example = "128")
		long totalUsers,

		@Schema(description = "Total number of platform administrators.", example = "1")
		long totalAdministrators,

		@Schema(description = "Total number of business owners.", example = "32")
		long totalOwners,

		@Schema(description = "Total number of workers.", example = "84")
		long totalWorkers,

		@Schema(description = "Total number of affiliates.", example = "11")
		long totalAffiliates,

		@Schema(description = "Total number of registered businesses.", example = "32")
		long totalBusinesses
) {
}
