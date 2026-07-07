package com.king_sparkon_tracker.backend.ai.tool;

import java.util.List;
import org.springframework.ai.tool.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TipsReadOnlyAiTool {

    private final DbReadOnlyLookupSupport lookupSupport;

    public TipsReadOnlyAiTool(DbReadOnlyLookupSupport lookupSupport) {
        this.lookupSupport = lookupSupport;
    }

    @Tool(description = "Read-only lookup for worker tip records. This tool only reads safe summary data.")
    public String lookupTips(
            @ToolParam(description = "Worker name, tip reference, status, or business text") String query,
            ToolContext toolContext) {
        if (!authenticated(toolContext)) {
            return "Tips lookup needs authenticated backend context. I can explain tip workflows publicly, but I cannot expose private tip records without authentication.";
        }

        return lookupSupport.lookup(
                "tips",
                List.of("tips", "tip", "worker_tips", "tip_records", "transactions"),
                query,
                5
        );
    }

    private boolean authenticated(ToolContext toolContext) {
        Object actor = toolContext == null ? null : toolContext.getContext().get("actor");
        return actor != null && StringUtils.hasText(String.valueOf(actor)) && !"anonymous".equalsIgnoreCase(String.valueOf(actor));
    }
}
