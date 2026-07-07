package com.king_sparkon_tracker.backend.ai.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DbReadOnlyLookupSupport {

    private static final Set<String> SAFE_COLUMN_HINTS = Set.of(
            "id", "name", "title", "reference", "code", "status", "email", "amount", "total", "created", "updated",
            "product", "ticket", "event", "worker", "business", "owner", "quantity", "price", "description"
    );

    private final JdbcTemplate jdbcTemplate;

    public DbReadOnlyLookupSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String lookup(String domain, List<String> candidateTables, String query, int maxRows) {
        String table = firstExistingTable(candidateTables);
        if (!StringUtils.hasText(table)) {
            return "No " + domain + " table was found in the current database schema.";
        }

        List<String> columns = safeColumns(table);
        if (columns.isEmpty()) {
            return "The " + domain + " table exists, but no safe readable columns were found.";
        }

        List<Map<String, Object>> rows = findRows(table, columns, query, Math.max(1, Math.min(maxRows, 5)));
        if (rows.isEmpty()) {
            return "No matching " + domain + " records were found for the supplied read-only lookup.";
        }

        return rows.stream()
                .map(row -> formatRow(domain, row))
                .collect(Collectors.joining("\n"));
    }

    public String countSummary(String domain, Map<String, List<String>> tablesByLabel) {
        Map<String, Long> counts = new LinkedHashMap<>();
        tablesByLabel.forEach((label, candidates) -> {
            String table = firstExistingTable(candidates);
            if (StringUtils.hasText(table)) {
                counts.put(label, count(table));
            }
        });

        if (counts.isEmpty()) {
            return "No dashboard tables were found for " + domain + ".";
        }

        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private String firstExistingTable(List<String> candidateTables) {
        for (String candidateTable : candidateTables) {
            if (tableExists(candidateTable)) {
                return candidateTable;
            }
        }
        return null;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private List<String> safeColumns(String tableName) {
        List<String> columns = jdbcTemplate.query(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position",
                (rs, rowNum) -> rs.getString("column_name"),
                tableName
        );

        return columns.stream()
                .filter(this::isSafeColumn)
                .limit(12)
                .toList();
    }

    private boolean isSafeColumn(String columnName) {
        String normalized = columnName.toLowerCase(Locale.ROOT);
        if (normalized.contains("password") || normalized.contains("secret") || normalized.contains("token") || normalized.contains("hash")) {
            return false;
        }
        return SAFE_COLUMN_HINTS.stream().anyMatch(normalized::contains);
    }

    private List<Map<String, Object>> findRows(String tableName, List<String> columns, String query, int maxRows) {
        String selectColumns = columns.stream()
                .map(column -> '"' + column + '"')
                .collect(Collectors.joining(", "));

        if (!StringUtils.hasText(query)) {
            return jdbcTemplate.queryForList("SELECT " + selectColumns + " FROM \"" + tableName + "\" LIMIT " + maxRows);
        }

        List<String> searchableColumns = columns.stream()
                .filter(column -> !column.toLowerCase(Locale.ROOT).contains("amount"))
                .limit(8)
                .toList();

        if (searchableColumns.isEmpty()) {
            return jdbcTemplate.queryForList("SELECT " + selectColumns + " FROM \"" + tableName + "\" LIMIT " + maxRows);
        }

        String whereClause = searchableColumns.stream()
                .map(column -> "LOWER(CAST(\"" + column + "\" AS TEXT)) LIKE ?")
                .collect(Collectors.joining(" OR "));
        Object[] args = searchableColumns.stream()
                .map(column -> "%" + query.toLowerCase(Locale.ROOT).trim() + "%")
                .toArray();

        return jdbcTemplate.queryForList("SELECT " + selectColumns + " FROM \"" + tableName + "\" WHERE " + whereClause + " LIMIT " + maxRows, args);
    }

    private long count(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tableName + "\"", Long.class);
        return count == null ? 0 : count;
    }

    private String formatRow(String domain, Map<String, Object> row) {
        List<String> parts = new ArrayList<>();
        row.forEach((key, value) -> {
            if (value != null) {
                parts.add(key + "=" + compact(String.valueOf(value)));
            }
        });
        return domain + " record: " + String.join(", ", parts);
    }

    private String compact(String value) {
        String compacted = value.replaceAll("\\s+", " ").trim();
        if (compacted.length() <= 120) {
            return compacted;
        }
        return compacted.substring(0, 120) + "...";
    }
}
