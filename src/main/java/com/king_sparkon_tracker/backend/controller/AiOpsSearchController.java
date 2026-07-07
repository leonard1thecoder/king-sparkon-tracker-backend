package com.king_sparkon_tracker.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AiOpsSearchResponse;
import com.king_sparkon_tracker.backend.service.AiOpsSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/ai/ops")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "AI operations search", description = "Read-only AI search, filtering, and summaries across products, tips, and tickets.")
public class AiOpsSearchController {

	private final AiOpsSearchService aiOpsSearchService;

	public AiOpsSearchController(AiOpsSearchService aiOpsSearchService) {
		this.aiOpsSearchService = aiOpsSearchService;
	}

	@GetMapping("/search")
	@Operation(summary = "Search and filter operations data", description = "Read-only search over products, tips, tickets, or all supported domains with AI summary.")
	public AiOpsSearchResponse search(
			@Parameter(description = "products, tips, tickets, ticker, or all") @RequestParam(defaultValue = "all") String domain,
			@Parameter(description = "Free-text search query") @RequestParam(required = false) String q,
			@Parameter(description = "Status filter") @RequestParam(required = false) String status,
			@Parameter(description = "Business id filter") @RequestParam(required = false) Long businessId,
			@Parameter(description = "Worker id filter") @RequestParam(required = false) Long workerId,
			@Parameter(description = "Product id filter") @RequestParam(required = false) Long productId,
			@Parameter(description = "Ticket reference, QR code, or reference filter") @RequestParam(required = false) String ticketReference,
			@Parameter(description = "Maximum rows to return, capped at 100") @RequestParam(defaultValue = "25") Integer limit) {
		return aiOpsSearchService.search(domain, q, status, businessId, workerId, productId, ticketReference, limit);
	}

	@GetMapping("/tickets")
	@Operation(summary = "Search tickets with AI", description = "Shortcut for read-only ticket search and AI summary.")
	public AiOpsSearchResponse searchTickets(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long businessId,
			@RequestParam(required = false) String reference,
			@RequestParam(defaultValue = "25") Integer limit) {
		return aiOpsSearchService.search("tickets", q, status, businessId, null, null, reference, limit);
	}

	@GetMapping("/products")
	@Operation(summary = "Search products with AI", description = "Shortcut for read-only product search and AI summary.")
	public AiOpsSearchResponse searchProducts(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long businessId,
			@RequestParam(required = false) Long productId,
			@RequestParam(defaultValue = "25") Integer limit) {
		return aiOpsSearchService.search("products", q, status, businessId, null, productId, null, limit);
	}

	@GetMapping("/tips")
	@Operation(summary = "Search tips with AI", description = "Shortcut for read-only tip search and AI summary.")
	public AiOpsSearchResponse searchTips(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long workerId,
			@RequestParam(defaultValue = "25") Integer limit) {
		return aiOpsSearchService.search("tips", q, status, null, workerId, null, null, limit);
	}
}
