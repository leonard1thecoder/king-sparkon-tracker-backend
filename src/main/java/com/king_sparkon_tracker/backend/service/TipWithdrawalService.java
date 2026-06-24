package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.king_sparkon_tracker.backend.dto.PayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.WorkerPayoutAccount;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.repository.TipWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.WorkerPayoutAccountRepository;

@Service
public class TipWithdrawalService {

	private static final int MONEY_SCALE = 2;

	private final TipRepository tipRepository;
	private final WorkerPayoutAccountRepository payoutAccountRepository;
	private final TipWithdrawalRepository withdrawalRepository;
	private final PriceLocalizationService priceLocalizationService;
	private final NotificationService notificationService;
	private final TrackerUserService trackerUserService;
	private final BigDecimal minimumWithdrawalAmount;
	private final int holdDays;
	private final String defaultOnboardingUrl;
	private final BigDecimal withdrawalFeePercent;
	private final BigDecimal additionalWithdrawalFeePercent;

	public TipWithdrawalService(
			TipRepository tipRepository,
			WorkerPayoutAccountRepository payoutAccountRepository,
			TipWithdrawalRepository withdrawalRepository,
			PriceLocalizationService priceLocalizationService,
			NotificationService notificationService,
			TrackerUserService trackerUserService,
			@Value("${app.tips.withdrawal-minimum-zar:1000}") BigDecimal minimumWithdrawalAmount,
			@Value("${app.tips.withdrawal-hold-days:7}") int holdDays,
			@Value("${app.tips.paypal-onboarding-url:http://localhost:3000/dashboard/owner/paypal/onboarding}") String defaultOnboardingUrl,
			@Value("${app.tips.withdrawal-fee-percent:8.5}") BigDecimal withdrawalFeePercent,
			@Value("${app.tips.additional-withdrawal-fee-percent:2.03}") BigDecimal additionalWithdrawalFeePercent) {
		this.tipRepository = tipRepository;
		this.payoutAccountRepository = payoutAccountRepository;
		this.withdrawalRepository = withdrawalRepository;
		this.priceLocalizationService = priceLocalizationService;
		this.notificationService = notificationService;
		this.trackerUserService = trackerUserService;
		this.minimumWithdrawalAmount = normalizeMoney(minimumWithdrawalAmount);
		this.holdDays = holdDays;
		this.defaultOnboardingUrl = defaultOnboardingUrl;
		this.withdrawalFeePercent = withdrawalFeePercent;
		this.additionalWithdrawalFeePercent = additionalWithdrawalFeePercent;
	}

	@Transactional
	public PayPalAccountResponse onboardPayPalAccount(PayPalAccountOnboardingRequest request, String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		TrackerUser worker = workerForOwner(request.workerId(), ownerUsername);
		String paypalEmail = request.paypalEmail().trim().toLowerCase(Locale.ROOT);
		String onboardingToken = UUID.randomUUID().toString();
		String onboardingUrl = onboardingUrl(request.callbackUrl(), request.workerId(), onboardingToken);

		WorkerPayoutAccount account = payoutAccountRepository.findByWorker_Id(request.workerId())
				.map(existing -> {
					existing.update(owner, paypalEmail, onboardingToken, onboardingUrl);
					return existing;
				})
				.orElseGet(() -> new WorkerPayoutAccount(worker, owner, paypalEmail, onboardingToken, onboardingUrl));

		return PayPalAccountResponse.from(payoutAccountRepository.save(account));
	}

	@Transactional(readOnly = true)
	public WithdrawalEligibilityResponse eligibility(Long workerId, String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		TrackerUser worker = workerForOwner(workerId, ownerUsername);
		List<Tip> eligibleTips = eligibleTips(worker.getId());
		BigDecimal availableAmount = availableNetAmount(eligibleTips);
		boolean paypalAccountReady = payoutAccountRepository.findByWorker_Id(worker.getId()).isPresent();
		boolean canWithdraw = paypalAccountReady
				&& availableAmount.compareTo(minimumWithdrawalAmount) >= 0;

		return new WithdrawalEligibilityResponse(
				worker.getId(),
				availableAmount,
				priceLocalizationService.localize(availableAmount, owner),
				priceLocalizationService.localize(minimumWithdrawalAmount, owner),
				eligibleTips.size(),
				holdDays,
				paypalAccountReady,
				canWithdraw,
				withdrawableBefore());
	}

	@Transactional
	public WithdrawalResponse requestWithdrawal(WithdrawalRequest request, String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		TrackerUser worker = workerForOwner(request.workerId(), ownerUsername);
		WorkerPayoutAccount account = payoutAccountRepository.findByWorker_Id(worker.getId())
				.orElseThrow(() -> new IllegalArgumentException("PayPal payout account onboarding is required before withdrawal"));

		List<Tip> eligibleTips = eligibleTips(worker.getId());
		BigDecimal amount = availableNetAmount(eligibleTips);
		if (amount.compareTo(minimumWithdrawalAmount) < 0) {
			throw new IllegalArgumentException("Available tip balance must be at least R1000.00 before withdrawal");
		}

		TipWithdrawal withdrawal = withdrawalRepository.save(new TipWithdrawal(
				worker,
				owner,
				amount,
				"ZAR",
				eligibleTips.size(),
				account.getPaypalEmail()));

		for (Tip tip : eligibleTips) {
			tip.assignWithdrawal(withdrawal);
			tipRepository.save(tip);
		}

		notificationService.logWithdrawalRequested(withdrawal);
		return WithdrawalResponse.from(withdrawal, priceLocalizationService.localize(withdrawal.getAmount(), owner));
	}

	@Transactional(readOnly = true)
	public List<WithdrawalResponse> getWithdrawalsForWorker(Long workerId, String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		TrackerUser worker = workerForOwner(workerId, ownerUsername);
		return withdrawalRepository.findByWorker_IdOrderByRequestedAtDesc(worker.getId())
				.stream()
				.map(withdrawal -> WithdrawalResponse.from(withdrawal, priceLocalizationService.localize(withdrawal.getAmount(), owner)))
				.toList();
	}

	private List<Tip> eligibleTips(Long workerId) {
		return tipRepository.findByWorker_IdAndStatusAndWithdrawalIsNullAndCreatedLessThanEqualOrderByCreatedDesc(
				workerId,
				TipStatus.PAID,
				withdrawableBefore());
	}

	private OffsetDateTime withdrawableBefore() {
		return OffsetDateTime.now().minusDays(holdDays);
	}

	private BigDecimal availableNetAmount(List<Tip> tips) {
		return tips.stream()
				.map(tip -> netAmount(tip.getTipAmount()))
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal netAmount(BigDecimal tipAmount) {
		BigDecimal normalizedTipAmount = normalizeMoney(tipAmount);
		BigDecimal effectiveWithdrawalFeePercent = withdrawalFeePercent.add(additionalWithdrawalFeePercent);
		BigDecimal withdrawalFee = normalizedTipAmount
				.multiply(effectiveWithdrawalFeePercent)
				.divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
		return normalizedTipAmount.subtract(withdrawalFee).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String onboardingUrl(String callbackUrl, Long workerId, String onboardingToken) {
		String baseUrl = callbackUrl == null || callbackUrl.isBlank()
				? defaultOnboardingUrl
				: callbackUrl.trim();

		return UriComponentsBuilder.fromUriString(baseUrl)
				.queryParam("workerId", workerId)
				.queryParam("token", URLEncoder.encode(onboardingToken, StandardCharsets.UTF_8))
				.build(true)
				.toUriString();
	}

	private TrackerUser owner(String ownerUsername) {
		TrackerUser owner = trackerUserService.getUserByUsername(ownerUsername);
		if (owner.getPrivilege() == null || owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can manage tip withdrawals");
		}
		return owner;
	}

	private TrackerUser workerForOwner(Long workerId, String ownerUsername) {
		TrackerUser worker = trackerUserService.getUserById(workerId, ownerUsername);
		if (worker.getPrivilege() == null || worker.getPrivilege().getName() != PrivilegeRole.Worker) {
			throw new IllegalArgumentException("Tip withdrawals can only target worker accounts");
		}
		return worker;
	}
}
