package com.king_sparkon_tracker.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.FullAiCandidateResponse;
import com.king_sparkon_tracker.backend.dto.FullAiSearchResponse;
import com.king_sparkon_tracker.backend.service.FullKingSparkonAiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/ai/full")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Full King Sparkon AI", description = "Read-only AI search and guidance for users, jobs, affiliates, business prospects, products, tips, and tickets.")
public class FullKingSparkonAiController {

	private final FullKingSparkonAiService fullKingSparkonAiService;

	public FullKingSparkonAiController(FullKingSparkonAiService fullKingSparkonAiService) {
		this.fullKingSparkonAiService = fullKingSparkonAiService;
	}

	@GetMapping("/search")
	@Operation(summary = "Search Full King Sparkon AI domains")
	public FullAiSearchResponse search(
			@RequestParam(defaultValue = "all") String domain,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long businessId,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Long affiliateId,
			@RequestParam(required = false) Long applicationId,
			@RequestParam(required = false) String reference,
			@RequestParam(defaultValue = "25") Integer limit) {
		return fullKingSparkonAiService.search(domain, q, status, businessId, userId, affiliateId, applicationId, reference, limit);
	}

	@GetMapping("/users")
	@Operation(summary = "Search users with Full King Sparkon AI")
	public FullAiSearchResponse users(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long userId,
			@RequestParam(defaultValue = "25") Integer limit) {
		return fullKingSparkonAiService.search("users", q, status, null, userId, null, null, null, limit);
	}

	@GetMapping("/jobs")
	@Operation(summary = "Search job posts and applications with Full King Sparkon AI")
	public FullAiSearchResponse jobs(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Long applicationId,
			@RequestParam(defaultValue = "25") Integer limit) {
		return fullKingSparkonAiService.search("jobs", q, status, null, userId, null, applicationId, null, limit);
	}

	@GetMapping("/jobs/candidate-review")
	@Operation(summary = "Review a CV or application against job evidence")
	public FullAiCandidateResponse candidateReview(
			@RequestParam(required = false) Long jobPostId,
			@RequestParam(required = false) Long applicationId,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) String q) {
		return fullKingSparkonAiService.reviewCandidate(jobPostId, applicationId, userId, q);
	}

	@GetMapping("/affiliates")
	@Operation(summary = "Search affiliates with Full King Sparkon AI")
	public FullAiSearchResponse affiliates(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long affiliateId,
			@RequestParam(defaultValue = "25") Integer limit) {
		return fullKingSparkonAiService.search("affiliates", q, status, null, null, affiliateId, null, null, limit);
	}

	@GetMapping("/affiliates/prospects")
	@Operation(summary = "Show affiliate business prospects and AI niche guidance")
	public FullAiSearchResponse affiliateProspects(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long businessId,
			@RequestParam(required = false) Long affiliateId,
			@RequestParam(defaultValue = "25") Integer limit) {
		return fullKingSparkonAiService.search("affiliate-prospects", q, status, businessId, null, affiliateId, null, null, limit);
	}
}
