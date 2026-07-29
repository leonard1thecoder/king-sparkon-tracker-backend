package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.BillingDashboardResponse;
import com.king_sparkon_tracker.backend.dto.BillingPlanResponse;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.service.BusinessBillingService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/billing")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BusinessBillingController {

	private final BusinessBillingService businessBillingService;

	public BusinessBillingController(BusinessBillingService businessBillingService) {
		this.businessBillingService = businessBillingService;
	}

	@GetMapping("/plans")
	public List<BillingPlanResponse> plans() {
		return businessBillingService.plans();
	}

	@GetMapping("/me")
	public BusinessBillingResponse currentBilling(Principal principal) {
		return businessBillingService.currentBilling(principal.getName());
	}

	@GetMapping("/dashboard")
	public BillingDashboardResponse dashboard(Principal principal) {
		return businessBillingService.dashboard(principal.getName());
	}

}
