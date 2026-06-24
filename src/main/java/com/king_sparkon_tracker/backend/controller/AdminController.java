package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AdminBusinessResponse;
import com.king_sparkon_tracker.backend.dto.AdminOverviewResponse;
import com.king_sparkon_tracker.backend.dto.CreatePromotionRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.dto.PromotionResponse;
import com.king_sparkon_tracker.backend.dto.UserResponse;
import com.king_sparkon_tracker.backend.service.AdministratorService;
import com.king_sparkon_tracker.backend.service.PromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Administration", description = "Platform administrator oversight APIs.")
public class AdminController {

	private final AdministratorService administratorService;
	private final PromotionService promotionService;

	public AdminController(AdministratorService administratorService, PromotionService promotionService) {
		this.administratorService = administratorService;
		this.promotionService = promotionService;
	}

	@GetMapping("/overview")
	@Operation(summary = "Admin overview", description = "Returns platform-level totals for administrators.")
	public AdminOverviewResponse overview(@Parameter(hidden = true) Principal principal) {
		return administratorService.overview(principal.getName());
	}

	@PostMapping("/promotions/registered-subscribers")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create registered-subscriber promotion", description = "Admin-only endpoint for creating a promotion for registered affiliate/subscriber users.")
	public PromotionResponse createRegisteredSubscriberPromotion(
			@Valid @RequestBody CreatePromotionRequest request,
			@Parameter(hidden = true) Principal principal) {
		return PromotionResponse.from(promotionService.createAdminRegisteredSubscriberPromotion(request, principal.getName()));
	}

	@GetMapping("/users")
	@Operation(summary = "List all users", description = "Admin-only endpoint for viewing every administrator, owner, worker, and affiliate.")
	public PageResponse<UserResponse> listUsers(
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size from 1 to 100.") @RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) Principal principal) {
		return PageResponse.from(
				administratorService.listUsers(pageable(page, size), principal.getName()),
				UserResponse::from);
	}

	@GetMapping("/users/{id}")
	@Operation(summary = "Get any user by id", description = "Admin-only endpoint for viewing one user across the whole platform.")
	public UserResponse getUserById(
			@Parameter(description = "User id.") @PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return UserResponse.from(administratorService.getUserById(id, principal.getName()));
	}

	@GetMapping("/businesses")
	@Operation(summary = "List all businesses", description = "Admin-only endpoint for viewing every business tenant.")
	public PageResponse<AdminBusinessResponse> listBusinesses(
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size from 1 to 100.") @RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) Principal principal) {
		return PageResponse.from(
				administratorService.listBusinesses(pageable(page, size), principal.getName()),
				AdminBusinessResponse::from);
	}

	@GetMapping("/businesses/{id}")
	@Operation(summary = "Get any business by id", description = "Admin-only endpoint for inspecting a business tenant.")
	public AdminBusinessResponse getBusinessById(
			@Parameter(description = "Business id.") @PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return AdminBusinessResponse.from(administratorService.getBusinessById(id, principal.getName()));
	}

	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}
