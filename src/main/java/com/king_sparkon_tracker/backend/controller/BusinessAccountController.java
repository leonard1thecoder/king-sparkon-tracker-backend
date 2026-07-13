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
import com.king_sparkon_tracker.backend.dto.OwnerWalletDtos.OwnerWalletSummaryResponse;
import com.king_sparkon_tracker.backend.dto.OwnerWalletDtos.OwnerWithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.OwnerWalletDtos.OwnerWithdrawalResponse;
import com.king_sparkon_tracker.backend.service.BusinessAccountService;
import com.king_sparkon_tracker.backend.service.OwnerWalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/business-account")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Business Account", description = "Unified owner wallet for online products, ticket sales, paid tips, promotions, billing top-ups, and withdrawals.")
public class BusinessAccountController {

	private final BusinessAccountService businessAccountService;
	private final OwnerWalletService ownerWalletService;

	public BusinessAccountController(BusinessAccountService businessAccountService) {
		this(businessAccountService, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	public BusinessAccountController(
			BusinessAccountService businessAccountService,
			OwnerWalletService ownerWalletService) {
		this.businessAccountService = businessAccountService;
		this.ownerWalletService = ownerWalletService;
	}

	@GetMapping("/summary")
	@Operation(summary = "Business account summary", description = "Returns owner business account balance and recent ledger entries.")
	public BusinessAccountSummaryResponse summary(Principal principal) {
		return businessAccountService.summary(principal.getName());
	}

	@GetMapping("/wallet")
	@Operation(summary = "Unified owner wallet", description = "Reconciles paid app product orders, successful ticket payments and paid tips into one owner balance.")
	public OwnerWalletSummaryResponse wallet(Principal principal) {
		return requiredWalletService().summary(principal.getName());
	}

	@GetMapping("/ledger")
	@Operation(summary = "Business account ledger", description = "Lists all business account ledger entries newest-first.")
	public List<BusinessAccountLedgerEntryResponse> ledger(Principal principal) {
		return businessAccountService.ledger(principal.getName());
	}

	@GetMapping("/withdrawals")
	@Operation(summary = "List owner withdrawals", description = "Lists unified and legacy product, tip and ticket withdrawals for the authenticated business.")
	public List<OwnerWithdrawalResponse> withdrawals(Principal principal) {
		return requiredWalletService().withdrawals(principal.getName());
	}

	@PostMapping("/withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Request unified owner withdrawal", description = "Debits the unified business wallet. The minimum owner withdrawal is R100.")
	public OwnerWithdrawalResponse requestWithdrawal(
			@Valid @RequestBody OwnerWithdrawalRequest request,
			Principal principal) {
		return requiredWalletService().requestWithdrawal(request, principal.getName());
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

	private OwnerWalletService requiredWalletService() {
		if (ownerWalletService == null) {
			throw new IllegalStateException("Owner wallet service is unavailable");
		}
		return ownerWalletService;
	}
}
