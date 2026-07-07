package com.king_sparkon_tracker.backend.ai.tool;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Describes safe read-only capabilities available to the assistant.
 * This does not mutate tickets, payments, products, or users.
 */
@Service
public class DefaultAiReadOnlyToolContextService implements AiReadOnlyToolContextService {

    @Override
    public List<String> availableToolContext(AiChatRequest request) {
        String message = request == null || request.message() == null
                ? ""
                : request.message().toLowerCase(Locale.ROOT);

        List<String> tools = new ArrayList<>();
        tools.add("Read-only mode is active. The assistant may explain flows and use authenticated read-only tools, but it must not create, update, delete, pay, verify, withdraw, or mutate anything.");
        tools.add("Available authenticated read-only tools: TicketReadOnlyAiTool, ProductReadOnlyAiTool, TipsReadOnlyAiTool, DashboardReadOnlyAiTool.");

        if (message.contains("ticket") || message.contains("qr")) {
            tools.add("TicketReadOnlyAiTool can search safe ticket/event summaries when the request has authenticated backend context.");
        }

        if (message.contains("product") || message.contains("barcode") || message.contains("inventory")) {
            tools.add("ProductReadOnlyAiTool can search safe product/inventory summaries when the request has authenticated backend context.");
        }

        if (message.contains("dashboard") || message.contains("owner") || message.contains("business")) {
            tools.add("DashboardReadOnlyAiTool can return safe owner dashboard counts when the request has authenticated backend context.");
        }

        if (message.contains("tip") || message.contains("payment") || message.contains("withdraw")) {
            tools.add("TipsReadOnlyAiTool can search safe worker tip summaries when the request has authenticated backend context. The assistant must not perform payment or withdrawal actions.");
        }

        return List.copyOf(tools);
    }
}
