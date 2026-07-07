package com.king_sparkon_tracker.backend.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultAiReadOnlyToolContextServiceTest {

    private final DefaultAiReadOnlyToolContextService toolContextService = new DefaultAiReadOnlyToolContextService();

    @Test
    void availableToolContextAlwaysDeclaresReadOnlyMode() {
        AiChatRequest request = new AiChatRequest("conversation-123", "Help", List.of(), "/", "Guest");

        List<String> context = toolContextService.availableToolContext(request);

        assertThat(context)
                .anySatisfy(value -> assertThat(value).contains("Read-only mode is active"));
    }

    @Test
    void availableToolContextIncludesPaymentSafetyForPaymentQuestion() {
        AiChatRequest request = new AiChatRequest("conversation-123", "Can you withdraw payment tips?", List.of(), "/tips", "Owner");

        List<String> context = toolContextService.availableToolContext(request);

        assertThat(context)
                .anySatisfy(value -> assertThat(value).contains("must not perform payment or withdrawal actions"));
    }
}
