package com.king_sparkon_tracker.backend.controller;

import com.king_sparkon_tracker.backend.idempotency.IdempotentRequest;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalResponse;
import com.king_sparkon_tracker.backend.service.TransactionWithdrawalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions/withdrawals")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Transaction withdrawals", description = "Owner withdrawals for paid website-payment transaction balances.")
public class TransactionWithdrawalController {

	private final TransactionWithdrawalService withdrawalService;

	public TransactionWithdrawalController(TransactionWithdrawalService withdrawalService) {
		this.withdrawalService = withdrawalService;
	}

	@PostMapping("/paypal/onboarding")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Configure owner PayPal payout account", description = "Stores the owner PayPal account used for website-payment transaction withdrawals.")
	public OwnerPayPalAccountResponse onboardPayPalAccount(
			@Valid @RequestBody OwnerPayPalAccountOnboardingRequest request,
			@Parameter(hidden = true) Principal principal) {
		return withdrawalService.onboardPayPalAccount(request, principal.getName());
	}

	@GetMapping("/eligibility")
	@Operation(summary = "Check transaction withdrawal eligibility", description = "Returns eligible paid website-payment balance after hold period and 6.5% withdrawal fee.")
	public TransactionWithdrawalEligibilityResponse eligibility(@Parameter(hidden = true) Principal principal) {
		return withdrawalService.eligibility(principal.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Request transaction withdrawal", description = "Requests withdrawal of all eligible website-payment transactions, or a selected eligible subset.")
	@IdempotentRequest(scope = "transaction-withdrawal")
	public TransactionWithdrawalResponse requestWithdrawal(
			@Valid @RequestBody(required = false) TransactionWithdrawalRequest request,
			@Parameter(hidden = true) Principal principal) {
		return withdrawalService.requestWithdrawal(request, principal.getName());
	}

	@GetMapping
	@Operation(summary = "List transaction withdrawals", description = "Lists owner-requested website-payment transaction withdrawals for the authenticated business.")
	public List<TransactionWithdrawalResponse> listWithdrawals(@Parameter(hidden = true) Principal principal) {
		return withdrawalService.getWithdrawals(principal.getName());
	}
}
