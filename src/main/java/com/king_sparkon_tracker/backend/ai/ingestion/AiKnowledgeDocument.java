package com.king_sparkon_tracker.backend.ai.ingestion;

import java.util.Map;

public record AiKnowledgeDocument(
        String source,
        String category,
        String title,
        String content,
        Map<String, Object> metadata
) {
}
