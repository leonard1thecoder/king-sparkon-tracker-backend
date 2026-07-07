package com.king_sparkon_tracker.backend.controller;

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

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.DevHubDevelopmentRequestCreateRequest;
import com.king_sparkon_tracker.backend.dto.DevHubDevelopmentRequestDecisionRequest;
import com.king_sparkon_tracker.backend.dto.DevHubDevelopmentRequestResponse;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.model.DevHubRequestStatus;
import com.king_sparkon_tracker.backend.service.DevHubDevelopmentRequestService;
import com.king_sparkon_tracker.backend.service.DevHubPageableFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dev-hub/requests")
@Tag(name = "Dev Hub AI", description = "AI-powered development request pricing, plan generation, and accept/reject workflow.")
public class DevHubDevelopmentRequestController {

	private final DevHubDevelopmentRequestService service;
	private final DevHubPageableFactory pageableFactory;

	public DevHubDevelopmentRequestController(
			DevHubDevelopmentRequestService service,
			DevHubPageableFactory pageableFactory) {
		this.service = service;
		this.pageableFactory = pageableFactory;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a Dev Hub request and receive AI price and development plan")
	public DevHubDevelopmentRequestResponse create(@Valid @RequestBody DevHubDevelopmentRequestCreateRequest request) {
		return DevHubDevelopmentRequestResponse.from(service.create(request));
	}

	@GetMapping
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@Operation(summary = "Search, filter, sort, and page Dev Hub development requests")
	public PageResponse<DevHubDevelopmentRequestResponse> search(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction,
			@RequestParam(required = false, name = "q") String query,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) DevHubRequestStatus status) {
		String effectiveSearch = query == null || query.isBlank() ? search : query;
		Pageable pageable = pageableFactory.create(page, size, sortBy, direction);
		return PageResponse.from(service.search(pageable, status, effectiveSearch), DevHubDevelopmentRequestResponse::from);
	}

	@GetMapping("/{id}")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	public DevHubDevelopmentRequestResponse get(@PathVariable Long id) {
		return DevHubDevelopmentRequestResponse.from(service.get(id));
	}

	@PatchMapping("/{id}/accept")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	public DevHubDevelopmentRequestResponse accept(
			@PathVariable Long id,
			@Valid @RequestBody DevHubDevelopmentRequestDecisionRequest request) {
		return DevHubDevelopmentRequestResponse.from(service.accept(id, request));
	}

	@PatchMapping("/{id}/reject")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	public DevHubDevelopmentRequestResponse reject(
			@PathVariable Long id,
			@Valid @RequestBody DevHubDevelopmentRequestDecisionRequest request) {
		return DevHubDevelopmentRequestResponse.from(service.reject(id, request));
	}
}
