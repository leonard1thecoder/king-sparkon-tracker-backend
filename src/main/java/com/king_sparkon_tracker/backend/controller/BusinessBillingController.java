package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.BillingDashboardResponse;
import com.king_sparkon_tracker.backend.dto.BillingPlanResponse;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.dto.CreateBusinessSubscriptionRequest;
import com.king_sparkon_tracker.backend.dto.CreateStripeCheckoutSessionResponse;
import com.king_sparkon_tracker.backend.service.BusinessBillingService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

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

	@PostMapping("/subscriptions")
	@ResponseStatus(HttpStatus.CREATED)
	public BusinessBillingResponse createSubscription(
			@Valid @RequestBody CreateBusinessSubscriptionRequest request,
			Principal principal) {
		return businessBillingService.createSubscription(principal.getName(), request);
	}

	@PostMapping("/stripe/checkout-sessions")
	@ResponseStatus(HttpStatus.CREATED)
	public CreateStripeCheckoutSessionResponse createStripeCheckoutSession(
			@Valid @RequestBody CreateBusinessSubscriptionRequest request,
			Principal principal) {
		return businessBillingService.createStripeCheckoutSession(principal.getName(), request);
	}

	@PostMapping("/subscriptions/{subscriptionId}/activate")
	public BusinessBillingResponse activateSubscription(
			@PathVariable Long subscriptionId,
			Principal principal) {
		return businessBillingService.activateApprovedSubscription(principal.getName(), subscriptionId);
	}
}
