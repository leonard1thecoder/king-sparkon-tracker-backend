package com.king_sparkon_tracker.backend.ai.support;

import com.king_sparkon_tracker.backend.ai.dto.AiChatMessage;
import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Centralizes assistant instructions so the controller and service stay thin.
 */
@Component
public class KingSparkonPromptFactory {

    public String buildSystemPrompt(AiChatRequest request, List<String> retrievedContext, List<String> readOnlyToolContext) {
        String currentPage = StringUtils.hasText(request.currentPage()) ? request.currentPage() : "unknown page";
        String userPrivilege = StringUtils.hasText(request.userPrivilege()) ? request.userPrivilege() : "guest";
        String conversationId = StringUtils.hasText(request.conversationId()) ? request.conversationId() : "new-conversation";
        String contextBlock = formatBlock("Retrieved context", retrievedContext);
        String toolBlock = formatBlock("Read-only tool context", readOnlyToolContext);
        String historyBlock = formatHistory(request.history());

        return """
                You are King Sparkon Assistant for King Sparkon Tracker.

                Product context:
                - King Sparkon Tracker supports barcode inventory tracking.
                - It supports QR tickets, event verification, owner dashboards, worker flows, buyer access, affiliate marketing, jobs, Dev Hub, QA, CI/CD, cloud maintenance, worker tips, and audit-ready reports.
                - The platform protects role-based access and must not invent private user, payment, ticket, worker, or business data.

                User context:
                - Conversation ID: %s
                - Current page: %s
                - User privilege: %s

                %s

                %s

                %s

                Response rules:
                - Keep answers concise enough for a floating chatbot.
                - Be direct, practical, and confident.
                - Use the retrieved context when it is relevant.
                - Treat tool context as read-only. Do not perform mutations or claim private lookup was completed.
                - Do not claim an action was completed unless the backend actually completed it.
                - If the user asks for real account, ticket, payment, dashboard, or business data, explain that authenticated backend lookup is required.
                - Guide users toward barcode tracking, QR tickets, business dashboards, Dev Hub, QA, and cloud support.
                """.formatted(conversationId, currentPage, userPrivilege, contextBlock, toolBlock, historyBlock);
    }

    public String buildSystemPrompt(AiChatRequest request) {
        return buildSystemPrompt(request, List.of(), List.of());
    }

    private String formatBlock(String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return title + ": none";
        }

        StringBuilder builder = new StringBuilder(title).append(":\n");
        values.stream()
                .filter(StringUtils::hasText)
                .limit(8)
                .forEach(value -> builder.append("- ").append(value).append('\n'));
        return builder.toString().trim();
    }

    private String formatHistory(List<AiChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "Conversation history: none";
        }

        StringBuilder builder = new StringBuilder("Conversation history:\n");
        history.stream()
                .filter(message -> message != null && StringUtils.hasText(message.content()))
                .limit(12)
                .forEach(message -> builder
                        .append("- ")
                        .append(normalizeRole(message.role()))
                        .append(": ")
                        .append(message.content())
                        .append('\n'));
        return builder.toString().trim();
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }

        String normalizedRole = role.trim().toLowerCase(Locale.ROOT);
        if (normalizedRole.equals("assistant")) {
            return "assistant";
        }
        return "user";
    }
}
