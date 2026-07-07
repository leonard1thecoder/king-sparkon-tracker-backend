package com.king_sparkon_tracker.backend.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProductPageableFactory {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
			Map.entry("id", "id"),
			Map.entry("name", "name"),
			Map.entry("price", "price"),
			Map.entry("stock", "stockQuantity"),
			Map.entry("stockquantity", "stockQuantity"),
			Map.entry("barcode", "productBarcode"),
			Map.entry("productbarcode", "productBarcode"),
			Map.entry("category", "category"),
			Map.entry("status", "status")
	);

	public Pageable create(int page, int size, String sortBy, String direction) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		String safeSortBy = SORT_FIELDS.getOrDefault(normalize(sortBy), "id");
		Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
		return PageRequest.of(safePage, safeSize, Sort.by(safeDirection, safeSortBy));
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "id";
		}
		return value.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
	}
}
