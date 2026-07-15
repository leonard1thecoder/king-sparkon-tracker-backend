package com.king_sparkon_tracker.backend.controller;

import com.king_sparkon_tracker.backend.idempotency.IdempotentRequest;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AffiliateCommissionResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.AffiliateProfileResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateTipRequest;
import com.king_sparkon_tracker.backend.dto.AffiliateWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateWithdrawalResponse;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.service.AffiliateService;
import com.king_sparkon_tracker.backend.service.OnboardingProfileService;
import com.king_sparkon_tracker.backend.service.TipService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/affiliates")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Affiliates", description = "Affiliate onboarding, promotion QR, commissions, and withdrawals.")
public class AffiliateController {

	private final AffiliateService affiliateService;
	private final TipService tipService;
	private final OnboardingProfileService onboardingProfileService;

	public AffiliateController(AffiliateService affiliateService, TipService tipService, OnboardingProfileService onboardingProfileService) {
		this.affiliateService = affiliateService;
		this.tipService = tipService;
		this.onboardingProfileService = onboardingProfileService;
	}

	@GetMapping("/me")
	@Operation(summary = "Current affiliate profile", description = "Returns affiliate profile, promotion link, QR code URL, and profile picture.")
	public AffiliateProfileResponse profile(Principal principal) {
		return affiliateService.profile(principal.getName());
	}

	@PatchMapping("/me/onboarding")
	@Operation(summary = "Complete affiliate onboarding", description = "Stores affiliate address, cellphone number, PayPal payout link, and optional profile picture.")
	public AffiliateProfileResponse completeOnboarding(
			@Valid @RequestBody AffiliateOnboardingRequest request,
			Principal principal) {
		return onboardingProfileService.completeAffiliateOnboarding(request, principal.getName());
	}

	@GetMapping("/me/commissions")
	@Operation(summary = "List affiliate commissions", description = "Lists commissions earned from referred pricing-plan subscriptions.")
	public List<AffiliateCommissionResponse> commissions(Principal principal) {
		return affiliateService.commissions(principal.getName());
	}

	@PostMapping("/me/tips")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create affiliate tip payment", description = "Creates a tip payment link and QR code for the authenticated affiliate.")
	@IdempotentRequest(scope = "affiliate-tip-create")
	public TipResponse createTip(
			@Valid @RequestBody AffiliateTipRequest request,
			Principal principal) {
		return tipService.createAffiliateTip(request, principal.getName());
	}

	@GetMapping("/me/tips")
	@Operation(summary = "List affiliate tips", description = "Lists tips created for the authenticated affiliate.")
	public List<TipResponse> tips(Principal principal) {
		return tipService.getTipsForAffiliate(principal.getName());
	}

	@GetMapping("/me/tip-withdrawals/eligibility")
	@Operation(summary = "Check affiliate tip withdrawal eligibility", description = "Returns available paid affiliate tip balance and PayPal status.")
	public WithdrawalEligibilityResponse tipWithdrawalEligibility(Principal principal) {
		return affiliateService.tipWithdrawalEligibility(principal.getName());
	}

	@PostMapping("/me/tip-withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Request affiliate tip withdrawal", description = "Requests withdrawal for all available paid affiliate tips.")
	@IdempotentRequest(scope = "affiliate-tip-withdrawal")
	public WithdrawalResponse requestTipWithdrawal(Principal principal) {
		return affiliateService.requestTipWithdrawal(principal.getName());
	}

	@GetMapping("/me/tip-withdrawals")
	@Operation(summary = "List affiliate tip withdrawals", description = "Lists affiliate tip withdrawal requests newest-first.")
	public List<WithdrawalResponse> tipWithdrawals(Principal principal) {
		return affiliateService.tipWithdrawals(principal.getName());
	}

	@GetMapping("/me/withdrawals/eligibility")
	@Operation(summary = "Check affiliate withdrawal eligibility", description = "Returns available commission balance and PayPal onboarding status.")
	public AffiliateWithdrawalEligibilityResponse withdrawalEligibility(Principal principal) {
		return affiliateService.withdrawalEligibility(principal.getName());
	}

	@PostMapping("/me/withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Request affiliate withdrawal", description = "Requests withdrawal for all available affiliate commissions.")
	@IdempotentRequest(scope = "affiliate-withdrawal")
	public AffiliateWithdrawalResponse requestWithdrawal(Principal principal) {
		return affiliateService.requestWithdrawal(principal.getName());
	}

	@GetMapping("/me/withdrawals")
	@Operation(summary = "List affiliate withdrawals", description = "Lists affiliate withdrawal requests newest-first.")
	public List<AffiliateWithdrawalResponse> withdrawals(Principal principal) {
		return affiliateService.withdrawals(principal.getName());
	}
}
