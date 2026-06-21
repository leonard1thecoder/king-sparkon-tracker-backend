package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AdminBusinessOverrideRequest;
import com.king_sparkon_tracker.backend.dto.BillingAuditLogResponse;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.service.AdminBusinessOverrideService;
import com.king_sparkon_tracker.backend.service.BillingAuditService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/businesses")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminBusinessBillingController {

	private final AdminBusinessOverrideService adminBusinessOverrideService;
	private final BillingAuditService billingAuditService;

	public AdminBusinessBillingController(
			AdminBusinessOverrideService adminBusinessOverrideService,
			BillingAuditService billingAuditService) {
		this.adminBusinessOverrideService = adminBusinessOverrideService;
		this.billingAuditService = billingAuditService;
	}

	@PostMapping("/{businessId}/billing/override")
	public BusinessBillingResponse overrideBusinessBilling(
			@PathVariable Long businessId,
			@Valid @RequestBody AdminBusinessOverrideRequest request,
			Principal principal) {
		return adminBusinessOverrideService.overrideBusiness(
				businessId,
				request,
				principal.getName()
		);
	}

	@GetMapping("/{businessId}/billing/audit-logs")
	public List<BillingAuditLogResponse> billingAuditLogs(@PathVariable Long businessId) {
		return billingAuditService.logsForBusiness(businessId);
	}
}
