package com.king_sparkon_tracker.backend.ai.exception;

/**
 * Raised when the configured AI provider cannot process a chatbot request.
 */
public class AiChatException extends RuntimeException {

    public AiChatException(String message) {
        super(message);
    }

    public AiChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
