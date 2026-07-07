package com.king_sparkon_tracker.backend.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

class DbReadOnlyLookupSupportTest {

    @Test
    void lookupReturnsSafeMatchingRowsOnly() {
        EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(database);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS public");
        jdbcTemplate.execute("CREATE TABLE public.products (id BIGINT, product_name VARCHAR(80), password_hash VARCHAR(80), status VARCHAR(20))");
        jdbcTemplate.update("INSERT INTO public.products (id, product_name, password_hash, status) VALUES (1, 'Scanner Pro', 'secret-hash', 'ACTIVE')");

        DbReadOnlyLookupSupport lookupSupport = new DbReadOnlyLookupSupport(jdbcTemplate);

        String result = lookupSupport.lookup("product", List.of("products"), "Scanner", 5);

        assertThat(result)
                .contains("product record")
                .contains("Scanner Pro")
                .contains("ACTIVE")
                .doesNotContain("secret-hash");

        database.shutdown();
    }
}
