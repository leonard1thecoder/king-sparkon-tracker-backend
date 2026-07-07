package com.king_sparkon_tracker.backend.ai.tool;

import java.util.List;
import org.springframework.ai.tool.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TicketReadOnlyAiTool {

    private final DbReadOnlyLookupSupport lookupSupport;

    public TicketReadOnlyAiTool(DbReadOnlyLookupSupport lookupSupport) {
        this.lookupSupport = lookupSupport;
    }

    @Tool(description = "Read-only lookup for ticket or event records. Requires authenticated context for private ticket data and never verifies, creates, pays, or updates tickets.")
    public String lookupTicket(
            @ToolParam(description = "Ticket reference, QR reference, event name, or buyer-visible ticket text") String query,
            ToolContext toolContext) {
        if (!authenticated(toolContext)) {
            return "Ticket lookup needs authenticated backend context. I can explain ticket flows publicly, but I cannot expose private ticket records without authentication.";
        }

        return lookupSupport.lookup(
                "ticket",
                List.of("tickets", "ticket", "ticket_events", "ticket_event", "event_tickets", "ticket_purchases", "ticket_purchase"),
                query,
                5
        );
    }

    private boolean authenticated(ToolContext toolContext) {
        Object actor = toolContext == null ? null : toolContext.getContext().get("actor");
        return actor != null && StringUtils.hasText(String.valueOf(actor)) && !"anonymous".equalsIgnoreCase(String.valueOf(actor));
    }
}
