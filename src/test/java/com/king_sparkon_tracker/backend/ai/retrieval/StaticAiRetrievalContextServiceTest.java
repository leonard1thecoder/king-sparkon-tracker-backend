package com.king_sparkon_tracker.backend.ai.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaticAiRetrievalContextServiceTest {

    private final StaticAiRetrievalContextService retrievalContextService = new StaticAiRetrievalContextService();

    @Test
    void retrieveContextIncludesTicketContextForQrQuestion() {
        AiChatRequest request = new AiChatRequest(
                "conversation-123",
                "How does QR ticket scanning work?",
                List.of(),
                "/tickets/scan",
                "Worker"
        );

        List<String> context = retrievalContextService.retrieveContext(request);

        assertThat(context)
                .anySatisfy(value -> assertThat(value).contains("scan-first business platform"))
                .anySatisfy(value -> assertThat(value).contains("QR ticket support"));
    }

    @Test
    void retrieveContextIncludesBarcodeContextForInventoryQuestion() {
        AiChatRequest request = new AiChatRequest(
                "conversation-123",
                "Explain barcode inventory tracking",
                List.of(),
                "/",
                "Owner"
        );

        List<String> context = retrievalContextService.retrieveContext(request);

        assertThat(context)
                .anySatisfy(value -> assertThat(value).contains("Barcode support"));
    }
}
