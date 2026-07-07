package com.king_sparkon_tracker.backend.dto;

import java.util.List;
import java.util.Map;

public record FullAiSearchResponse(
		String domain,
		String query,
		Map<String, String> filters,
		int resultCount,
		List<Map<String, Object>> rows,
		String aiSummary
) {
}
