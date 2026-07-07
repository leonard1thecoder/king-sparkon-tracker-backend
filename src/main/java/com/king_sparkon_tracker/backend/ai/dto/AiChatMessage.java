package com.king_sparkon_tracker.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One compact message from the frontend chat history.
 */
public record AiChatMessage(
        @NotBlank(message = "role is required")
        @Size(max = 30, message = "role must not exceed 30 characters")
        String role,

        @NotBlank(message = "content is required")
        @Size(max = 2_000, message = "content must not exceed 2000 characters")
        String content
) {
}
