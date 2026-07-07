package com.king_sparkon_tracker.backend.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class DevHubPageableFactory {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
			Map.entry("id", "id"),
			Map.entry("client", "clientName"),
			Map.entry("clientname", "clientName"),
			Map.entry("email", "emailAddress"),
			Map.entry("company", "companyName"),
			Map.entry("companyname", "companyName"),
			Map.entry("projecttype", "projectType"),
			Map.entry("title", "title"),
			Map.entry("status", "status"),
			Map.entry("minprice", "estimatedMinPrice"),
			Map.entry("maxprice", "estimatedMaxPrice"),
			Map.entry("created", "createdAt"),
			Map.entry("createdat", "createdAt"),
			Map.entry("updated", "updatedAt"),
			Map.entry("updatedat", "updatedAt")
	);

	public Pageable create(int page, int size, String sortBy, String direction) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		String safeSortBy = SORT_FIELDS.getOrDefault(normalize(sortBy), "createdAt");
		Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return PageRequest.of(safePage, safeSize, Sort.by(safeDirection, safeSortBy));
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "createdat";
		}
		return value.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
	}
}
