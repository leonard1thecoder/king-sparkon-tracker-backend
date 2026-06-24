package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.AffiliateLinkResponse;
import com.king_sparkon_tracker.backend.dto.CreateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.dto.UpdateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.service.AffiliateLinkService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/affiliate-links")
public class AffiliateLinkController {

	private final AffiliateLinkService affiliateLinkService;

	public AffiliateLinkController(AffiliateLinkService affiliateLinkService) {
		this.affiliateLinkService = affiliateLinkService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AffiliateLinkResponse create(@Valid @RequestBody CreateAffiliateLinkRequest request) {
		return affiliateLinkService.create(request);
	}

	@PatchMapping("/{id}")
	public AffiliateLinkResponse update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateAffiliateLinkRequest request) {
		return affiliateLinkService.update(id, request);
	}

	@GetMapping
	public PageResponse<AffiliateLinkResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(affiliateLinkService.list(pageable(page, size)), response -> response);
	}

	@GetMapping("/random")
	public List<AffiliateLinkResponse> randomForAuthenticatedUser(
			@RequestParam AffiliatePlacement placement,
			@RequestParam(defaultValue = "1") int limit,
			@RequestParam(required = false) Integer triggerCount,
			Principal principal) {
		return affiliateLinkService.randomForAuthenticated(placement, triggerCount, limit, principal.getName());
	}

	@GetMapping("/public/random")
	public List<AffiliateLinkResponse> randomForPublicClient(
			@RequestParam AffiliatePlacement placement,
			@RequestParam(defaultValue = "1") int limit,
			@RequestParam(required = false) Integer triggerCount,
			@RequestParam(required = false) Long workerId,
			@RequestParam(required = false) BusinessPlan plan) {
		return affiliateLinkService.randomForPublicClient(placement, triggerCount, limit, workerId, plan);
	}

	@PostMapping("/{id}/click")
	public AffiliateLinkResponse recordClick(@PathVariable Long id) {
		return affiliateLinkService.recordClick(id);
	}

	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}
