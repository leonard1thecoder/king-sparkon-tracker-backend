package com.king_sparkon_tracker.backend.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body consumed by the floating chatbot UI.
 */
public record AiChatRequest(
        @Size(max = 120, message = "conversationId must not exceed 120 characters")
        String conversationId,

        @NotBlank(message = "message is required")
        @Size(max = 2_000, message = "message must not exceed 2000 characters")
        String message,

        @Valid
        @Size(max = 12, message = "history must not exceed 12 messages")
        List<AiChatMessage> history,

        @Size(max = 200, message = "currentPage must not exceed 200 characters")
        String currentPage,

        @Size(max = 80, message = "userPrivilege must not exceed 80 characters")
        String userPrivilege
) {
}
