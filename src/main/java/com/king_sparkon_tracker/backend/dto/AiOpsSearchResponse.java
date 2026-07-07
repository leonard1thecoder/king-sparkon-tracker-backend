package com.king_sparkon_tracker.backend.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiOpsSearchResponse(
		@Schema(description = "Requested search domain.", example = "products")
		String domain,

		@Schema(description = "Free-text query used for search.", example = "coke")
		String query,

		@Schema(description = "Filters applied to the search.")
		Map<String, String> filters,

		@Schema(description = "Rows returned after filtering.", example = "12")
		int resultCount,

		@Schema(description = "Read-only result rows with sensitive columns removed.")
		List<Map<String, Object>> rows,

		@Schema(description = "AI generated summary of the search results.")
		String aiSummary
) {
}
