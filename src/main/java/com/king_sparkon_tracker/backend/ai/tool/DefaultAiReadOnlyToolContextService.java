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
        tools.add("Read-only mode is active. The assistant may explain flows and guide the user, but it must not create, update, delete, pay, verify, or withdraw anything.");

        if (message.contains("ticket") || message.contains("qr")) {
            tools.add("Available read-only ticket guidance: explain how buyers get tickets, how workers scan QR codes, and when authenticated ticket lookup is required.");
        }

        if (message.contains("product") || message.contains("barcode") || message.contains("inventory")) {
            tools.add("Available read-only barcode guidance: explain product scanning, stock movement, audit trails, and when authenticated product lookup is required.");
        }

        if (message.contains("dashboard") || message.contains("owner") || message.contains("business")) {
            tools.add("Available read-only dashboard guidance: explain owner dashboard sections, worker visibility, reports, and role access without exposing private account data.");
        }

        if (message.contains("tip") || message.contains("payment") || message.contains("withdraw")) {
            tools.add("Available read-only payment guidance: explain tips, withdrawals, holds, and fees. The assistant must not perform payment or withdrawal actions.");
        }

        return List.copyOf(tools);
    }
}
