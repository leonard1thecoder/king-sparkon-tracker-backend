package com.king_sparkon_tracker.backend.ai.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response returned to the frontend floating chatbot.
 */
public record AiChatResponse(
        String conversationId,
        String answer,
        String model,
        String source,
        List<String> suggestions,
        Instant createdAt
) {
}
