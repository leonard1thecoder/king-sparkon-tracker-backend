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
import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountLedgerEntryResponse;
import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountSummaryResponse;
import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountTopUpRequest;
import com.king_sparkon_tracker.backend.service.BusinessAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/business-account")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Business Account", description = "In-app owner wallet for promotion and ticket boost spend.")
public class BusinessAccountController {

	private final BusinessAccountService businessAccountService;

	public BusinessAccountController(BusinessAccountService businessAccountService) {
		this.businessAccountService = businessAccountService;
	}

	@GetMapping("/summary")
	@Operation(summary = "Business account summary", description = "Returns owner business account balance and recent ledger entries.")
	public BusinessAccountSummaryResponse summary(Principal principal) {
		return businessAccountService.summary(principal.getName());
	}

	@GetMapping("/ledger")
	@Operation(summary = "Business account ledger", description = "Lists all business account ledger entries newest-first.")
	public List<BusinessAccountLedgerEntryResponse> ledger(Principal principal) {
		return businessAccountService.ledger(principal.getName());
	}

	@PostMapping("/top-ups")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create top-up", description = "Creates an in-app Stripe top-up checkout link for the owner business account.")
	public BusinessAccountLedgerEntryResponse createTopUp(
			@Valid @RequestBody BusinessAccountTopUpRequest request,
			Principal principal) {
		return businessAccountService.createTopUp(request, principal.getName());
	}

	@PostMapping("/top-ups/{entryId}/confirm")
	@Operation(summary = "Confirm top-up", description = "Marks a completed top-up as posted. Webhook wiring can call the same service method later.")
	public BusinessAccountLedgerEntryResponse confirmTopUp(
			@PathVariable Long entryId,
			Principal principal) {
		return businessAccountService.confirmTopUp(entryId, principal.getName());
	}
}
