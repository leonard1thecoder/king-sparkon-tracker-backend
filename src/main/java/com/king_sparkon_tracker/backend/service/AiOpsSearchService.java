package com.king_sparkon_tracker.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.AiOpsSearchResponse;

@Service
@Transactional(readOnly = true)
public class AiOpsSearchService {

	private static final Logger log = LoggerFactory.getLogger(AiOpsSearchService.class);
	private static final int DEFAULT_LIMIT = 25;
	private static final int MAX_LIMIT = 100;
	private static final String SCHEMA = "public";
	private static final Set<String> SENSITIVE_COLUMN_PARTS = Set.of(
			"password",
			"secret",
			"token",
			"hash",
			"credential",
			"webhook"
	);
	private static final Set<String> SEARCH_HINTS = Set.of(
			"id",
			"name",
			"title",
			"barcode",
			"unit",
			"reference",
			"status",
			"email",
			"amount",
			"price",
			"quantity",
			"created",
			"updated",
			"worker",
			"business",
			"product",
			"ticket",
			"event",
			"qr",
			"payment"
	);

	private final JdbcTemplate jdbcTemplate;
	private final ChatClient chatClient;

	public AiOpsSearchService(JdbcTemplate jdbcTemplate, ChatClient chatClient) {
		this.jdbcTemplate = jdbcTemplate;
		this.chatClient = chatClient;
	}

	public AiOpsSearchResponse search(
			String domain,
			String query,
			String status,
			Long businessId,
			Long workerId,
			Long productId,
			String ticketReference,
			Integer limit) {
		String normalizedDomain = normalizeDomain(domain);
		String normalizedQuery = normalizeOptional(query);
		String normalizedStatus = normalizeOptional(status);
		String normalizedTicketReference = normalizeOptional(ticketReference);
		int safeLimit = safeLimit(limit);
		Map<String, String> filters = filters(normalizedStatus, businessId, workerId, productId, normalizedTicketReference, safeLimit);
		List<Map<String, Object>> rows = new ArrayList<>();

		for (String table : candidateTables(normalizedDomain)) {
			if (rows.size() >= safeLimit) {
				break;
			}
			rows.addAll(searchTable(
					normalizedDomain,
					table,
					normalizedQuery,
					normalizedStatus,
					businessId,
					workerId,
					productId,
					normalizedTicketReference,
					safeLimit - rows.size()));
		}

		String summary = summarize(normalizedDomain, normalizedQuery, filters, rows);
		return new AiOpsSearchResponse(normalizedDomain, normalizedQuery, filters, rows.size(), rows, summary);
	}

	private List<Map<String, Object>> searchTable(
			String domain,
			String table,
			String query,
			String status,
			Long businessId,
			Long workerId,
			Long productId,
			String ticketReference,
			int limit) {
		if (limit <= 0 || !tableExists(table)) {
			return List.of();
		}

		List<String> columns = safeColumns(table);
		if (columns.isEmpty()) {
			return List.of();
		}

		List<String> where = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		addTextSearch(where, args, columns, query);
		addColumnFilter(where, args, columns, "status", status);
		addLongFilter(where, args, columns, "business_id", businessId);
		addLongFilter(where, args, columns, "worker_id", workerId);
		addLongFilter(where, args, columns, "product_id", productId);
		addTextColumnLikeFilter(where, args, columns, "ticket_reference", ticketReference);
		addTextColumnLikeFilter(where, args, columns, "reference", ticketReference);
		addTextColumnLikeFilter(where, args, columns, "qr_code", ticketReference);

		String selectColumns = String.join(", ", columns.stream().map(this::quote).toList());
		StringBuilder sql = new StringBuilder("select ")
				.append(selectColumns)
				.append(" from ")
				.append(quote(table));
		if (!where.isEmpty()) {
			sql.append(" where ").append(String.join(" and ", where));
		}
		if (columns.contains("created") || columns.contains("created_at")) {
			sql.append(" order by ").append(quote(columns.contains("created") ? "created" : "created_at")).append(" desc");
		} else if (columns.contains("id")) {
			sql.append(" order by ").append(quote("id")).append(" desc");
		}
		sql.append(" limit ?");
		args.add(limit);

		try {
			return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
					.map(row -> row(domain, table, row))
					.toList();
		} catch (RuntimeException exception) {
			log.warn("ai_ops_search_table_failed domain={} table={} reason={}", domain, table, exception.getMessage());
			return List.of();
		}
	}

	private Map<String, Object> row(String domain, String table, Map<String, Object> row) {
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("domain", domain);
		output.put("table", table);
		row.forEach((key, value) -> {
			if (!isSensitive(key)) {
				output.put(key, value);
			}
		});
		return output;
	}

	private void addTextSearch(List<String> where, List<Object> args, List<String> columns, String query) {
		if (!StringUtils.hasText(query)) {
			return;
		}
		List<String> searchable = columns.stream()
				.filter(column -> SEARCH_HINTS.stream().anyMatch(column.toLowerCase(Locale.ROOT)::contains))
				.limit(12)
				.toList();
		if (searchable.isEmpty()) {
			return;
		}
		where.add("(" + String.join(" or ", searchable.stream()
				.map(column -> "lower(cast(" + quote(column) + " as text)) like ?")
				.toList()) + ")");
		for (int i = 0; i < searchable.size(); i++) {
			args.add("%" + query.toLowerCase(Locale.ROOT) + "%");
		}
	}

	private void addColumnFilter(List<String> where, List<Object> args, List<String> columns, String column, String value) {
		if (!StringUtils.hasText(value) || !columns.contains(column)) {
			return;
		}
		where.add("lower(cast(" + quote(column) + " as text)) = ?");
		args.add(value.toLowerCase(Locale.ROOT));
	}

	private void addLongFilter(List<String> where, List<Object> args, List<String> columns, String column, Long value) {
		if (value == null || !columns.contains(column)) {
			return;
		}
		where.add(quote(column) + " = ?");
		args.add(value);
	}

	private void addTextColumnLikeFilter(List<String> where, List<Object> args, List<String> columns, String column, String value) {
		if (!StringUtils.hasText(value) || !columns.contains(column)) {
			return;
		}
		where.add("lower(cast(" + quote(column) + " as text)) like ?");
		args.add("%" + value.toLowerCase(Locale.ROOT) + "%");
	}

	private boolean tableExists(String table) {
		Integer count = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_schema = ? and table_name = ?",
				Integer.class,
				SCHEMA,
				table);
		return count != null && count > 0;
	}

	private List<String> safeColumns(String table) {
		return jdbcTemplate.queryForList(
				"select column_name from information_schema.columns where table_schema = ? and table_name = ? order by ordinal_position",
				String.class,
				SCHEMA,
				table)
				.stream()
				.filter(column -> !isSensitive(column))
				.limit(24)
				.toList();
	}

	private boolean isSensitive(String column) {
		String normalized = column.toLowerCase(Locale.ROOT);
		return SENSITIVE_COLUMN_PARTS.stream().anyMatch(normalized::contains);
	}

	private List<String> candidateTables(String domain) {
		Set<String> tables = new LinkedHashSet<>();
		if ("all".equals(domain) || "products".equals(domain)) {
			tables.addAll(List.of("products", "product_barcodes", "barcode_catalog", "inventory_transactions", "transaction_items"));
		}
		if ("all".equals(domain) || "tips".equals(domain)) {
			tables.addAll(List.of("tips", "tip_withdrawals", "worker_payout_accounts"));
		}
		if ("all".equals(domain) || "tickets".equals(domain)) {
			tables.addAll(List.of("ticket_events", "tickets", "ticket_purchases", "ticket_verifications", "event_tickets", "ticket_boosts", "ticket_qr_codes"));
		}
		return new ArrayList<>(tables);
	}

	private Map<String, String> filters(String status, Long businessId, Long workerId, Long productId, String ticketReference, int limit) {
		Map<String, String> filters = new LinkedHashMap<>();
		if (StringUtils.hasText(status)) filters.put("status", status);
		if (businessId != null) filters.put("businessId", String.valueOf(businessId));
		if (workerId != null) filters.put("workerId", String.valueOf(workerId));
		if (productId != null) filters.put("productId", String.valueOf(productId));
		if (StringUtils.hasText(ticketReference)) filters.put("ticketReference", ticketReference);
		filters.put("limit", String.valueOf(limit));
		return filters;
	}

	private String summarize(String domain, String query, Map<String, String> filters, List<Map<String, Object>> rows) {
		String fallback = rows.isEmpty()
				? "No matching " + domain + " records were found for the supplied filters."
				: "Found " + rows.size() + " " + domain + " records. Review the rows for exact stored values before taking action.";
		try {
			return chatClient.prompt()
					.system("""
						You are King Sparkon Operations AI.
						Summarize read-only search results for products, tips, and tickets in two short sentences.
						Never invent values, never mutate data, never mark tips paid, never verify tickets, and never change inventory.
						Use only the rows shown.
						""")
					.user("domain=%s%nquery=%s%nfilters=%s%nrows=%s".formatted(domain, query, filters, rows.stream().limit(10).toList()))
					.call()
					.content();
		} catch (RuntimeException exception) {
			log.warn("ai_ops_summary_failed_non_blocking domain={} reason={}", domain, exception.getMessage());
			return fallback;
		}
	}

	private String normalizeDomain(String domain) {
		String normalized = normalizeOptional(domain);
		if (normalized == null) {
			return "all";
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "product", "products", "inventory" -> "products";
			case "tip", "tips" -> "tips";
			case "ticket", "tickets", "ticker" -> "tickets";
			default -> "all";
		};
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private int safeLimit(Integer limit) {
		if (limit == null) {
			return DEFAULT_LIMIT;
		}
		return Math.max(1, Math.min(MAX_LIMIT, limit));
	}

	private String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}
}
