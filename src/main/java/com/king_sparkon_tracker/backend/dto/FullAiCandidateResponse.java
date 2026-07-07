package com.king_sparkon_tracker.backend.dto;

import java.util.List;
import java.util.Map;

public record FullAiCandidateResponse(
		Long jobPostId,
		Long applicationId,
		Long userId,
		int evidenceCount,
		List<Map<String, Object>> evidence,
		String summary,
		String explanation
) {
}
