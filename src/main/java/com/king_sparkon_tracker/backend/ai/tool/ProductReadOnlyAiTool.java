package com.king_sparkon_tracker.backend.ai.tool;

import java.util.List;
import org.springframework.ai.tool.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductReadOnlyAiTool {

    private final DbReadOnlyLookupSupport lookupSupport;

    public ProductReadOnlyAiTool(DbReadOnlyLookupSupport lookupSupport) {
        this.lookupSupport = lookupSupport;
    }

    @Tool(description = "Read-only lookup for product and inventory records. Never changes stock or updates products.")
    public String lookupProduct(
            @ToolParam(description = "Product name, product code, product id, or inventory text") String query,
            ToolContext toolContext) {
        if (!authenticated(toolContext)) {
            return "Product lookup needs authenticated backend context. I can explain inventory workflows publicly, but I cannot expose private product records without authentication.";
        }

        return lookupSupport.lookup(
                "product",
                List.of("products", "product", "barcodes", "barcode", "company_products", "company_product", "inventory_items", "inventory"),
                query,
                5
        );
    }

    private boolean authenticated(ToolContext toolContext) {
        Object actor = toolContext == null ? null : toolContext.getContext().get("actor");
        return actor != null && StringUtils.hasText(String.valueOf(actor)) && !"anonymous".equalsIgnoreCase(String.valueOf(actor));
    }
}
