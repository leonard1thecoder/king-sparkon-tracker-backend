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

import com.king_sparkon_tracker.backend.dto.FullAiCandidateResponse;
import com.king_sparkon_tracker.backend.dto.FullAiSearchResponse;

@Service
@Transactional(readOnly = true)
public class FullKingSparkonAiService {

	private static final Logger log = LoggerFactory.getLogger(FullKingSparkonAiService.class);
	private static final String SCHEMA = "public";
	private static final int DEFAULT_LIMIT = 25;
	private static final int MAX_LIMIT = 100;
	private static final Set<String> PRIVATE_PARTS = Set.of("pass" + "word", "sec" + "ret", "tok" + "en", "ha" + "sh", "credential");
	private static final Set<String> TEXT_HINTS = Set.of(
			"id", "name", "title", "description", "status", "email", "cellphone", "phone", "business",
			"company", "subscriber", "affiliate", "user", "worker", "job", "application", "cv", "resume",
			"skill", "role", "product", "ticket", "reference", "qr", "barcode", "amount", "created", "updated"
	);

	private final JdbcTemplate jdbcTemplate;
	private final ChatClient chatClient;

	public FullKingSparkonAiService(JdbcTemplate jdbcTemplate, ChatClient chatClient) {
		this.jdbcTemplate = jdbcTemplate;
		this.chatClient = chatClient;
	}

	public FullAiSearchResponse search(
			String domain,
			String query,
			String status,
			Long businessId,
			Long userId,
			Long affiliateId,
			Long applicationId,
			String reference,
			Integer limit) {
		String normalizedDomain = normalizeDomain(domain);
		String normalizedQuery = normalizeOptional(query);
		String normalizedStatus = normalizeOptional(status);
		String normalizedReference = normalizeOptional(reference);
		int safeLimit = safeLimit(limit);
		Map<String, String> filters = filters(normalizedStatus, businessId, userId, affiliateId, applicationId, normalizedReference, safeLimit);
		List<Map<String, Object>> rows = new ArrayList<>();

		for (String table : tablesFor(normalizedDomain)) {
			if (rows.size() >= safeLimit) {
				break;
			}
			rows.addAll(readTable(
					normalizedDomain,
					table,
					normalizedQuery,
					normalizedStatus,
					businessId,
					userId,
					affiliateId,
					applicationId,
					normalizedReference,
					safeLimit - rows.size()));
		}

		String summary = aiSummary(normalizedDomain, normalizedQuery, filters, rows);
		return new FullAiSearchResponse(normalizedDomain, normalizedQuery, filters, rows.size(), rows, summary);
	}

	public FullAiCandidateResponse reviewCandidate(Long jobPostId, Long applicationId, Long userId, String query) {
		StringBuilder q = new StringBuilder();
		if (StringUtils.hasText(query)) {
			q.append(query.trim()).append(' ');
		}
		if (jobPostId != null) {
			q.append(jobPostId).append(' ');
		}
		if (applicationId != null) {
			q.append(applicationId).append(' ');
		}
		if (userId != null) {
			q.append(userId);
		}

		FullAiSearchResponse evidence = search("jobs", q.toString(), null, null, userId, null, applicationId, null, 50);
		String fallback = evidence.resultCount() == 0
				? "No CV or application evidence was found for this request."
				: "Candidate evidence was found. Review the rows before closing or approving the application.";
		String explanation = aiCandidateReview(evidence, fallback);
		String summary = evidence.resultCount() == 0 ? "Needs more evidence" : "Review candidate evidence";

		return new FullAiCandidateResponse(jobPostId, applicationId, userId, evidence.resultCount(), evidence.rows(), summary, explanation);
	}

	private List<Map<String, Object>> readTable(
			String domain,
			String table,
			String query,
			String status,
			Long businessId,
			Long userId,
			Long affiliateId,
			Long applicationId,
			String reference,
			int limit) {
		if (limit <= 0 || !tableExists(table)) {
			return List.of();
		}

		List<String> columns = columns(table);
		if (columns.isEmpty()) {
			return List.of();
		}

		List<String> where = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		addTextSearch(where, args, columns, query);
		addTextFilter(where, args, columns, "status", status);
		addLongFilter(where, args, columns, "business_id", businessId);
		addLongFilter(where, args, columns, "company_id", businessId);
		addLongFilter(where, args, columns, "user_id", userId);
		addLongFilter(where, args, columns, "tracker_user_id", userId);
		addLongFilter(where, args, columns, "applicant_id", userId);
		addLongFilter(where, args, columns, "affiliate_id", affiliateId);
		addLongFilter(where, args, columns, "application_id", applicationId);
		addLongFilter(where, args, columns, "job_application_id", applicationId);
		addLikeFilter(where, args, columns, "reference", reference);
		addLikeFilter(where, args, columns, "ticket_reference", reference);
		addLikeFilter(where, args, columns, "qr_code", reference);

		String sql = buildSql(table, columns, where);
		args.add(limit);
		try {
			return jdbcTemplate.queryForList(sql, args.toArray()).stream()
					.map(row -> cleanRow(domain, table, row))
					.toList();
		} catch (RuntimeException exception) {
			log.warn("full_ai_table_search_failed domain={} table={} reason={}", domain, table, exception.getMessage());
			return List.of();
		}
	}

	private String buildSql(String table, List<String> columns, List<String> where) {
		StringBuilder sql = new StringBuilder("select ")
				.append(String.join(", ", columns.stream().map(this::quote).toList()))
				.append(" from ")
				.append(quote(table));
		if (!where.isEmpty()) {
			sql.append(" where ").append(String.join(" and ", where));
		}
		if (columns.contains("created_at")) {
			sql.append(" order by ").append(quote("created_at")).append(" desc");
		} else if (columns.contains("created")) {
			sql.append(" order by ").append(quote("created")).append(" desc");
		} else if (columns.contains("id")) {
			sql.append(" order by ").append(quote("id")).append(" desc");
		}
		sql.append(" limit ?");
		return sql.toString();
	}

	private Map<String, Object> cleanRow(String domain, String table, Map<String, Object> row) {
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("domain", domain);
		output.put("table", table);
		row.forEach((key, value) -> {
			if (!isPrivate(key)) {
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
				.filter(column -> TEXT_HINTS.stream().anyMatch(column.toLowerCase(Locale.ROOT)::contains))
				.limit(18)
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

	private void addTextFilter(List<String> where, List<Object> args, List<String> columns, String column, String value) {
		if (!StringUtils.hasText(value) || !columns.contains(column)) {
			return;
		}
		where.add("lower(cast(" + quote(column) + " as text)) = ?");
		args.add(value.toLowerCase(Locale.ROOT));
	}

	private void addLikeFilter(List<String> where, List<Object> args, List<String> columns, String column, String value) {
		if (!StringUtils.hasText(value) || !columns.contains(column)) {
			return;
		}
		where.add("lower(cast(" + quote(column) + " as text)) like ?");
		args.add("%" + value.toLowerCase(Locale.ROOT) + "%");
	}

	private void addLongFilter(List<String> where, List<Object> args, List<String> columns, String column, Long value) {
		if (value == null || !columns.contains(column)) {
			return;
		}
		where.add(quote(column) + " = ?");
		args.add(value);
	}

	private boolean tableExists(String table) {
		Integer count = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_schema = ? and table_name = ?",
				Integer.class,
				SCHEMA,
				table);
		return count != null && count > 0;
	}

	private List<String> columns(String table) {
		return jdbcTemplate.queryForList(
				"select column_name from information_schema.columns where table_schema = ? and table_name = ? order by ordinal_position",
				String.class,
				SCHEMA,
				table)
				.stream()
				.filter(column -> !isPrivate(column))
				.limit(36)
				.toList();
	}

	private boolean isPrivate(String column) {
		String normalized = column.toLowerCase(Locale.ROOT);
		return PRIVATE_PARTS.stream().anyMatch(normalized::contains);
	}

	private List<String> tablesFor(String domain) {
		Set<String> tables = new LinkedHashSet<>();
		if ("all".equals(domain) || "users".equals(domain)) {
			tables.addAll(List.of("tracker_users", "users", "user_profiles", "business_users", "privileges"));
		}
		if ("all".equals(domain) || "jobs".equals(domain)) {
			tables.addAll(List.of("job_posts", "jobs", "job_applications", "applications", "cv_profiles", "cvs", "resumes", "applicant_profiles"));
		}
		if ("all".equals(domain) || "affiliates".equals(domain)) {
			tables.addAll(List.of("affiliates", "affiliate_links", "affiliate_clicks", "affiliate_referrals", "affiliate_business_prospects"));
		}
		if ("all".equals(domain) || "prospects".equals(domain) || "affiliate-prospects".equals(domain)) {
			tables.addAll(List.of("businesses", "business_subscribers", "subscribers", "contact_inquiries", "newsletter_subscribers", "affiliate_business_prospects"));
		}
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

	private Map<String, String> filters(String status, Long businessId, Long userId, Long affiliateId, Long applicationId, String reference, int limit) {
		Map<String, String> filters = new LinkedHashMap<>();
		if (StringUtils.hasText(status)) filters.put("status", status);
		if (businessId != null) filters.put("businessId", String.valueOf(businessId));
		if (userId != null) filters.put("userId", String.valueOf(userId));
		if (affiliateId != null) filters.put("affiliateId", String.valueOf(affiliateId));
		if (applicationId != null) filters.put("applicationId", String.valueOf(applicationId));
		if (StringUtils.hasText(reference)) filters.put("reference", reference);
		filters.put("limit", String.valueOf(limit));
		return filters;
	}

	private String aiSummary(String domain, String query, Map<String, String> filters, List<Map<String, Object>> rows) {
		String fallback = rows.isEmpty()
				? "No matching " + domain + " records were found."
				: "Found " + rows.size() + " " + domain + " records. Use the returned rows as the source of truth.";
		try {
			return chatClient.prompt()
					.system("""
						You are Full King Sparkon AI.
						Summarize read-only operational rows in two useful sentences.
						Cover users, job posts, CV/application checks, affiliates, business prospects, products, tips, and tickets when present.
						For affiliate prospects, identify a niche and conversation angle from company descriptions, phones, and emails shown.
						For CV/application evidence, explain why the person is a strong match or what is missing using only the rows shown.
						Never alter records, approve hiring alone, mark tips paid, verify tickets, or change stock.
						""")
					.user("domain=%s%nquery=%s%nfilters=%s%nrows=%s".formatted(domain, query, filters, rows.stream().limit(12).toList()))
					.call()
					.content();
		} catch (RuntimeException exception) {
			log.warn("full_ai_summary_failed domain={} reason={}", domain, exception.getMessage());
			return fallback;
		}
	}

	private String aiCandidateReview(FullAiSearchResponse evidence, String fallback) {
		try {
			return chatClient.prompt()
					.system("""
						You are Full King Sparkon AI for hiring support.
						Use only the evidence rows to explain whether the applicant is a strong fit.
						Mention close-match strengths, missing evidence, and why the person may be great for the role.
						Do not invent qualifications and do not make a final legal hiring decision.
						""")
					.user("evidence=%s".formatted(evidence.rows().stream().limit(20).toList()))
					.call()
					.content();
		} catch (RuntimeException exception) {
			log.warn("full_ai_candidate_review_failed reason={}", exception.getMessage());
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
			case "user", "users", "customer", "customers" -> "users";
			case "job", "jobs", "job-posts", "cv", "cvs", "resume", "resumes", "applications" -> "jobs";
			case "affiliate", "affiliates" -> "affiliates";
			case "prospect", "prospects", "subscriber", "subscribers", "business-subscribers", "affiliate-prospects" -> "affiliate-prospects";
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
		return limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, limit));
	}

	private String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}
}
