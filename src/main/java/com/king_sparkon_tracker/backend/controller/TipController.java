package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

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

import com.king_sparkon_tracker.backend.dto.PayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UpdateTipStatusRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.service.TipService;
import com.king_sparkon_tracker.backend.service.TipWithdrawalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tips")
public class TipController {

	private final TipService tipService;
	private final TipWithdrawalService withdrawalService;

	public TipController(TipService tipService, TipWithdrawalService withdrawalService) {
		this.tipService = tipService;
		this.withdrawalService = withdrawalService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TipResponse createTip(@Valid @RequestBody TipRequest request) {
		return tipService.createTip(request);
	}

	@PatchMapping("/{tipId}/status")
	public TipResponse updateTipStatus(
			@PathVariable Long tipId,
			@Valid @RequestBody UpdateTipStatusRequest request) {
		return tipService.updateTipStatus(tipId, request);
	}

	@GetMapping("/worker/{workerId}")
	public List<TipResponse> getTipsForWorker(@PathVariable Long workerId) {
		return tipService.getTipsForWorker(workerId);
	}

	@GetMapping
	public List<TipResponse> getTipsByStatus(@RequestParam TipStatus status) {
		return tipService.getTipsByStatus(status);
	}

	@PostMapping("/paypal/onboarding")
	@ResponseStatus(HttpStatus.CREATED)
	public PayPalAccountResponse onboardPayPalAccount(
			@Valid @RequestBody PayPalAccountOnboardingRequest request,
			Principal principal) {
		return withdrawalService.onboardPayPalAccount(request, principal.getName());
	}

	@GetMapping("/worker/{workerId}/withdrawals/eligibility")
	public WithdrawalEligibilityResponse withdrawalEligibility(@PathVariable Long workerId, Principal principal) {
		return withdrawalService.eligibility(workerId, principal.getName());
	}

	@GetMapping("/worker/{workerId}/withdrawals")
	public List<WithdrawalResponse> getWithdrawalsForWorker(@PathVariable Long workerId, Principal principal) {
		return withdrawalService.getWithdrawalsForWorker(workerId, principal.getName());
	}

	@PostMapping("/withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	public WithdrawalResponse requestWithdrawal(@Valid @RequestBody WithdrawalRequest request, Principal principal) {
		return withdrawalService.requestWithdrawal(request, principal.getName());
	}
}
