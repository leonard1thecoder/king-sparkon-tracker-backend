package com.king_sparkon_tracker.backend.ai.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.king_sparkon_tracker.backend.ai.dto.AiChatMessage;
import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class KingSparkonPromptFactoryTest {

    private final KingSparkonPromptFactory promptFactory = new KingSparkonPromptFactory();

    @Test
    void buildSystemPromptIncludesConversationPagePrivilegeContextToolsAndHistory() {
        AiChatRequest request = new AiChatRequest(
                "conversation-123",
                "How do QR tickets work?",
                List.of(new AiChatMessage("user", "I need ticket help")),
                "/tickets/scan",
                "Worker"
        );

        String prompt = promptFactory.buildSystemPrompt(
                request,
                List.of("QR ticket support focuses on verification."),
                List.of("Read-only mode is active.")
        );

        assertThat(prompt)
                .contains("Conversation ID: conversation-123")
                .contains("Current page: /tickets/scan")
                .contains("User privilege: Worker")
                .contains("QR ticket support focuses on verification.")
                .contains("Read-only mode is active.")
                .contains("user: I need ticket help")
                .contains("must not invent private user, payment, ticket, worker, or business data");
    }

    @Test
    void buildSystemPromptFallsBackWhenOptionalValuesAreMissing() {
        AiChatRequest request = new AiChatRequest(null, "Help", null, null, null);

        String prompt = promptFactory.buildSystemPrompt(request);

        assertThat(prompt)
                .contains("Conversation ID: new-conversation")
                .contains("Current page: unknown page")
                .contains("User privilege: guest")
                .contains("Conversation history: none");
    }
}
