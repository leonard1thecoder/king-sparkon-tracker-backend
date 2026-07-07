package com.king_sparkon_tracker.backend.ai.service.impl;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import com.king_sparkon_tracker.backend.ai.dto.AiChatResponse;
import com.king_sparkon_tracker.backend.ai.exception.AiChatException;
import com.king_sparkon_tracker.backend.ai.service.AiChatService;
import com.king_sparkon_tracker.backend.ai.support.KingSparkonPromptFactory;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SpringAiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatServiceImpl.class);

    private final ChatClient chatClient;
    private final KingSparkonPromptFactory promptFactory;
    private final String modelName;

    public SpringAiChatServiceImpl(
            ChatClient chatClient,
            KingSparkonPromptFactory promptFactory,
            @Value("${spring.ai.ollama.chat.model:qwen3:4b}") String modelName) {
        this.chatClient = chatClient;
        this.promptFactory = promptFactory;
        this.modelName = modelName;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new AiChatException("Chat message is required");
        }

        try {
            String answer = chatClient
                    .prompt()
                    .system(promptFactory.buildSystemPrompt(request))
                    .user(request.message())
                    .call()
                    .content();

            return new AiChatResponse(
                    answer,
                    modelName,
                    "SPRING_AI_OLLAMA",
                    buildSuggestions(request.message()),
                    Instant.now()
            );
        } catch (AiChatException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("AI chat request failed. model={}", modelName, exception);
            throw new AiChatException("AI assistant is temporarily unavailable", exception);
        }
    }

    private List<String> buildSuggestions(String message) {
        String normalizedMessage = message == null ? "" : message.toLowerCase();

        if (normalizedMessage.contains("ticket") || normalizedMessage.contains("qr")) {
            return List.of("How do I scan a ticket?", "How does owner ticket verification work?");
        }

        if (normalizedMessage.contains("barcode") || normalizedMessage.contains("inventory") || normalizedMessage.contains("scan")) {
            return List.of("How do I track stock movement?", "How does barcode audit trail work?");
        }

        if (normalizedMessage.contains("owner") || normalizedMessage.contains("business") || normalizedMessage.contains("dashboard")) {
            return List.of("Show owner dashboard features", "How does role access work?");
        }

        if (normalizedMessage.contains("dev") || normalizedMessage.contains("qa") || normalizedMessage.contains("cloud")) {
            return List.of("Explain Dev Hub support", "What cloud maintenance do you support?");
        }

        return List.of("Barcode tracking", "QR tickets", "Owner dashboard", "Dev Hub support");
    }
}
