package com.king_sparkon_tracker.backend.ai.tool;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            @ToolParam(description = "Product name, product code, product id, or inventory text") String query) {
        if (!authenticated()) {
            return "Product lookup needs authenticated backend context. I can explain inventory workflows publicly, but I cannot expose private product records without authentication.";
        }

        return lookupSupport.lookup(
                "product",
                List.of("products", "product", "barcodes", "barcode", "company_products", "company_product", "inventory_items", "inventory"),
                query,
                5
        );
    }

    private boolean authenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && StringUtils.hasText(authentication.getName())
                && !"anonymousUser".equals(authentication.getName());
    }
}
