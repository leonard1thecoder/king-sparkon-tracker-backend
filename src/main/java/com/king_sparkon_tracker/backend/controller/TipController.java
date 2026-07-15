package com.king_sparkon_tracker.backend.controller;

import com.king_sparkon_tracker.backend.idempotency.IdempotentRequest;
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

import com.king_sparkon_tracker.backend.dto.AiTipConfirmationResponse;
import com.king_sparkon_tracker.backend.dto.PayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UpdateTipStatusRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.service.AiTipConfirmationService;
import com.king_sparkon_tracker.backend.service.TipService;
import com.king_sparkon_tracker.backend.service.TipWithdrawalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tips")
public class TipController {

	private final TipService tipService;
	private final TipWithdrawalService withdrawalService;
	private final AiTipConfirmationService aiTipConfirmationService;

	public TipController(
			TipService tipService,
			TipWithdrawalService withdrawalService,
			AiTipConfirmationService aiTipConfirmationService) {
		this.tipService = tipService;
		this.withdrawalService = withdrawalService;
		this.aiTipConfirmationService = aiTipConfirmationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@IdempotentRequest(scope = "tip-create")
	public TipResponse createTip(@Valid @RequestBody TipRequest request, Principal principal) {
		principal.getName();
		return tipService.createTip(request);
	}

	@PatchMapping("/{tipId}/status")
	public TipResponse updateTipStatus(
			@PathVariable Long tipId,
			@Valid @RequestBody UpdateTipStatusRequest request,
			Principal principal) {
		return tipService.updateTipStatus(tipId, request, principal.getName());
	}

	@GetMapping("/{tipId}/ai-confirm")
	public AiTipConfirmationResponse aiConfirmTip(@PathVariable Long tipId, Principal principal) {
		return aiTipConfirmationService.confirmTipForOwner(tipId, principal.getName());
	}

	@GetMapping("/worker/{workerId}")
	public List<TipResponse> getTipsForWorker(@PathVariable Long workerId, Principal principal) {
		return tipService.getTipsForWorker(workerId, principal.getName());
	}

	@GetMapping("/worker/{workerId}/ai-confirm")
	public AiTipConfirmationResponse aiConfirmWorkerTips(@PathVariable Long workerId, Principal principal) {
		return aiTipConfirmationService.confirmWorkerTipsForOwner(workerId, principal.getName());
	}

	@GetMapping("/me")
	public List<TipResponse> getMyWorkerTips(Principal principal) {
		return tipService.getTipsForCurrentWorker(principal.getName());
	}

	@GetMapping("/owner")
	public List<TipResponse> getCurrentOwnerBusinessTips(Principal principal) {
		return tipService.getTipsForCurrentOwner(principal.getName());
	}

	@GetMapping("/me/ai-confirm")
	public AiTipConfirmationResponse aiConfirmMyWorkerTips(Principal principal) {
		return aiTipConfirmationService.confirmMyWorkerTips(principal.getName());
	}

	@GetMapping
	public List<TipResponse> getTipsByStatus(@RequestParam TipStatus status, Principal principal) {
		return tipService.getTipsByStatus(status, principal.getName());
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
	@IdempotentRequest(scope = "tip-withdrawal")
	public WithdrawalResponse requestWithdrawal(@Valid @RequestBody WithdrawalRequest request, Principal principal) {
		return withdrawalService.requestWithdrawal(request, principal.getName());
	}
}
