package com.king_sparkon_tracker.backend.ai.tool;

import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DashboardReadOnlyAiTool {

    private final DbReadOnlyLookupSupport lookupSupport;

    public DashboardReadOnlyAiTool(DbReadOnlyLookupSupport lookupSupport) {
        this.lookupSupport = lookupSupport;
    }

    @Tool(description = "Read-only owner dashboard summary. Returns safe counts only and never changes business data.")
    public String dashboardSummary(ToolContext toolContext) {
        if (!authenticated(toolContext)) {
            return "Dashboard summary needs authenticated backend context. I can explain dashboard sections publicly, but I cannot expose private dashboard data without authentication.";
        }

        return lookupSupport.countSummary(
                "owner dashboard",
                Map.of(
                        "businesses", List.of("businesses", "business"),
                        "products", List.of("products", "product", "company_products", "company_product"),
                        "tickets", List.of("tickets", "ticket", "ticket_events", "ticket_event"),
                        "tips", List.of("tips", "tip", "worker_tips", "tip_records"),
                        "transactions", List.of("transactions", "transaction")
                )
        );
    }

    private boolean authenticated(ToolContext toolContext) {
        Object actor = toolContext == null ? null : toolContext.getContext().get("actor");
        return actor != null && StringUtils.hasText(String.valueOf(actor)) && !"anonymous".equalsIgnoreCase(String.valueOf(actor));
    }
}
