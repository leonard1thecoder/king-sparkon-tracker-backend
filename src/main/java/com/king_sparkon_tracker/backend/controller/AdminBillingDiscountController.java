package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.BillingDiscountDtos.BillingDiscountResponse;
import com.king_sparkon_tracker.backend.dto.BillingDiscountDtos.UpsertBillingDiscountRequest;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.service.BillingPlanDiscountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/billing-discounts")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Admin billing discounts", description = "Administrator-managed Plus and Pro service discounts.")
public class AdminBillingDiscountController {

	private final BillingPlanDiscountService discountService;

	public AdminBillingDiscountController(BillingPlanDiscountService discountService) {
		this.discountService = discountService;
	}

	@GetMapping
	@Operation(summary = "List Plus and Pro discounts")
	public List<BillingDiscountResponse> list(@Parameter(hidden = true) Principal principal) {
		return discountService.listForAdmin(principal.getName());
	}

	@PutMapping("/{plan}")
	@Operation(summary = "Create or update Plus or Pro discount")
	public BillingDiscountResponse upsert(
			@PathVariable BusinessPlan plan,
			@Valid @RequestBody UpsertBillingDiscountRequest request,
			@Parameter(hidden = true) Principal principal) {
		return discountService.upsert(plan, request, principal.getName());
	}
}
