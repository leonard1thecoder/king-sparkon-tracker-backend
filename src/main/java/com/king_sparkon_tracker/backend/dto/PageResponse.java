package com.king_sparkon_tracker.backend.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

public record PageResponse<T>(
		@Schema(description = "Current page content.")
		List<T> content,
		@Schema(description = "Zero-based page number.", example = "0")
		int page,
		@Schema(description = "Page size.", example = "20")
		int size,
		@Schema(description = "Total matching elements.", example = "42")
		long totalElements,
		@Schema(description = "Total matching pages.", example = "3")
		int totalPages,
		@Schema(description = "True when this is the first page.", example = "true")
		boolean first,
		@Schema(description = "True when this is the last page.", example = "false")
		boolean last) {

	/**
	 * Maps a Spring Data page into the API's stable pagination envelope.
	 */
	public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
