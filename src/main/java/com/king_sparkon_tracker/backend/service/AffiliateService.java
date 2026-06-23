package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import com.king_sparkon_tracker.backend.dto.AffiliateCommissionResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.AffiliateProfileResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateWithdrawalResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.model.AffiliateCommission;
import com.king_sparkon_tracker.backend.model.AffiliateCommissionStatus;
import com.king_sparkon_tracker.backend.model.AffiliateWithdrawal;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.AffiliateCommissionRepository;
import com.king_sparkon_tracker.backend.repository.AffiliateWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.repository.TipWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class AffiliateService {

	private static final int MONEY_SCALE = 2;
	private static final BigDecimal FIRST_THREE_MONTHS_RATE = new BigDecimal("18.00");
	private static final BigDecimal AFTER_THREE_MONTHS_RATE = new BigDecimal("23.00");
	private static final BigDecimal AFTER_ONE_YEAR_RATE = new BigDecimal("28.00");

	private final TrackerUserRepository userRepository;
	private final AffiliateCommissionRepository commissionRepository;
	private final AffiliateWithdrawalRepository withdrawalRepository;
	private final TipRepository tipRepository;
	private final TipWithdrawalRepository tipWithdrawalRepository;
	private final PriceLocalizationService priceLocalizationService;
	private final NotificationService notificationService;
	private final Clock clock;
	private final BigDecimal tipMinimumWithdrawalAmount;
	private final int tipHoldDays;
	private final BigDecimal tipWithdrawalFeePercent;

	public AffiliateService(
			TrackerUserRepository userRepository,
			AffiliateCommissionRepository commissionRepository,
			AffiliateWithdrawalRepository withdrawalRepository,
			TipRepository tipRepository,
			TipWithdrawalRepository tipWithdrawalRepository,
			PriceLocalizationService priceLocalizationService,
			NotificationService notificationService,
			Clock clock,
			@Value("${app.affiliates.tips.withdrawal-minimum-zar:1000}") BigDecimal tipMinimumWithdrawalAmount,
			@Value("${app.affiliates.tips.withdrawal-hold-days:7}") int tipHoldDays,
			@Value("${app.affiliates.tips.withdrawal-fee-percent:8.5}") BigDecimal tipWithdrawalFeePercent) {
		this.userRepository = userRepository;
		this.commissionRepository = commissionRepository;
		this.withdrawalRepository = withdrawalRepository;
		this.tipRepository = tipRepository;
		this.tipWithdrawalRepository = tipWithdrawalRepository;
		this.priceLocalizationService = priceLocalizationService;
		this.notificationService = notificationService;
		this.clock = clock;
		this.tipMinimumWithdrawalAmount = normalizeMoney(tipMinimumWithdrawalAmount);
		this.tipHoldDays = tipHoldDays;
		this.tipWithdrawalFeePercent = tipWithdrawalFeePercent == null
				? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: tipWithdrawalFeePercent.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	@Transactional(readOnly = true)
	public AffiliateProfileResponse profile(String affiliateUsername) {
		return AffiliateProfileResponse.from(affiliate(affiliateUsername));
	}

	public AffiliateProfileResponse completeOnboarding(AffiliateOnboardingRequest request, String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		affiliate.completeAffiliateOnboarding(
				normalizeRequired(request.physicalAddress(), "Physical address is required"),
				normalizeRequired(request.cellphoneNumber(), "Cellphone number is required"),
				normalizeRequired(request.paypalLink(), "PayPal link is required"));
		return AffiliateProfileResponse.from(userRepository.save(affiliate));
	}

	public AffiliateCommission recordCommission(BusinessSubscription subscription, Business business, LocalDateTime earnedAt) {
		if (subscription == null || subscription.getId() == null || business == null || business.getAffiliate() == null) {
			return null;
		}
		if (commissionRepository.existsBySubscription_Id(subscription.getId())) {
			return null;
		}

		TrackerUser affiliate = business.getAffiliate();
		if (affiliate.getPrivilege() == null || affiliate.getPrivilege().getName() != PrivilegeRole.Affiliate) {
			return null;
		}

		BigDecimal grossAmount = normalizeMoney(subscription.getAmount());
		if (grossAmount.signum() <= 0) {
			return null;
		}

		LocalDateTime effectiveEarnedAt = earnedAt == null ? now() : earnedAt;
		BigDecimal commissionRate = commissionRatePercent(affiliate, effectiveEarnedAt);
		BigDecimal commissionAmount = grossAmount
				.multiply(commissionRate)
				.divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);

		AffiliateCommission commission = commissionRepository.save(new AffiliateCommission(
				affiliate,
				business,
				subscription,
				grossAmount,
				commissionRate,
				commissionAmount,
				subscription.getCurrency(),
				effectiveEarnedAt));
		notificationService.logAffiliateCommissionEarned(commission);
		return commission;
	}

	@Transactional(readOnly = true)
	public List<AffiliateCommissionResponse> commissions(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		return commissionRepository.findByAffiliate_IdOrderByEarnedAtDesc(affiliate.getId())
				.stream()
				.map(AffiliateCommissionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AffiliateWithdrawalEligibilityResponse withdrawalEligibility(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		List<AffiliateCommission> eligibleCommissions = eligibleCommissions(affiliate);
		BigDecimal availableAmount = availableAmount(eligibleCommissions);
		boolean paypalLinkReady = StringUtils.hasText(affiliate.getAffiliatePaypalLink());
		boolean canWithdraw = paypalLinkReady && availableAmount.signum() > 0;

		return new AffiliateWithdrawalEligibilityResponse(
				affiliate.getId(),
				availableAmount,
				priceLocalizationService.localize(availableAmount, affiliate),
				eligibleCommissions.size(),
				paypalLinkReady,
				canWithdraw,
				affiliate.getAffiliatePaypalLink());
	}

	public AffiliateWithdrawalResponse requestWithdrawal(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		String paypalLink = normalizeRequired(
				affiliate.getAffiliatePaypalLink(),
				"PayPal link is required before affiliate withdrawal");
		List<AffiliateCommission> eligibleCommissions = eligibleCommissions(affiliate);
		BigDecimal amount = availableAmount(eligibleCommissions);
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("Available affiliate commission balance must be greater than zero before withdrawal");
		}

		AffiliateWithdrawal withdrawal = withdrawalRepository.save(new AffiliateWithdrawal(
				affiliate,
				amount,
				"ZAR",
				eligibleCommissions.size(),
				paypalLink));

		for (AffiliateCommission commission : eligibleCommissions) {
			commission.assignWithdrawal(withdrawal);
		}
		commissionRepository.saveAll(eligibleCommissions);

		notificationService.logAffiliateWithdrawalRequested(withdrawal);
		return AffiliateWithdrawalResponse.from(withdrawal, priceLocalizationService.localize(amount, affiliate));
	}

	@Transactional(readOnly = true)
	public List<AffiliateWithdrawalResponse> withdrawals(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		return withdrawalRepository.findByAffiliate_IdOrderByRequestedAtDesc(affiliate.getId())
				.stream()
				.map(withdrawal -> AffiliateWithdrawalResponse.from(
						withdrawal,
						priceLocalizationService.localize(withdrawal.getAmount(), affiliate)))
				.toList();
	}

	@Transactional(readOnly = true)
	public WithdrawalEligibilityResponse tipWithdrawalEligibility(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		List<Tip> eligibleTips = eligibleAffiliateTips(affiliate);
		BigDecimal availableAmount = availableTipNetAmount(eligibleTips);
		boolean paypalLinkReady = StringUtils.hasText(affiliate.getAffiliatePaypalLink());
		boolean canWithdraw = paypalLinkReady && availableAmount.compareTo(tipMinimumWithdrawalAmount) >= 0;

		return new WithdrawalEligibilityResponse(
				affiliate.getId(),
				availableAmount,
				priceLocalizationService.localize(availableAmount, affiliate),
				priceLocalizationService.localize(tipMinimumWithdrawalAmount, affiliate),
				eligibleTips.size(),
				tipHoldDays,
				paypalLinkReady,
				canWithdraw,
				tipWithdrawableBefore());
	}

	public WithdrawalResponse requestTipWithdrawal(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		String paypalLink = normalizeRequired(
				affiliate.getAffiliatePaypalLink(),
				"PayPal link is required before affiliate tip withdrawal");
		List<Tip> eligibleTips = eligibleAffiliateTips(affiliate);
		BigDecimal amount = availableTipNetAmount(eligibleTips);
		if (amount.compareTo(tipMinimumWithdrawalAmount) < 0) {
			throw new IllegalArgumentException("Available affiliate tip balance must be at least R1000.00 before withdrawal");
		}

		TipWithdrawal withdrawal = tipWithdrawalRepository.save(new TipWithdrawal(
				affiliate,
				affiliate,
				amount,
				"ZAR",
				eligibleTips.size(),
				paypalLink));

		for (Tip tip : eligibleTips) {
			tip.assignWithdrawal(withdrawal);
		}
		tipRepository.saveAll(eligibleTips);

		notificationService.logWithdrawalRequested(withdrawal);
		return WithdrawalResponse.from(withdrawal, priceLocalizationService.localize(withdrawal.getAmount(), affiliate));
	}

	@Transactional(readOnly = true)
	public List<WithdrawalResponse> tipWithdrawals(String affiliateUsername) {
		TrackerUser affiliate = affiliate(affiliateUsername);
		return tipWithdrawalRepository.findByWorker_IdOrderByRequestedAtDesc(affiliate.getId())
				.stream()
				.map(withdrawal -> WithdrawalResponse.from(withdrawal, priceLocalizationService.localize(withdrawal.getAmount(), affiliate)))
				.toList();
	}

	BigDecimal commissionRatePercent(TrackerUser affiliate, LocalDateTime earnedAt) {
		LocalDateTime joinedAt = affiliate.getAffiliateJoinedAt() == null
				? affiliate.getCreatedDate()
				: affiliate.getAffiliateJoinedAt();
		if (joinedAt == null) {
			joinedAt = earnedAt;
		}

		long months = ChronoUnit.MONTHS.between(joinedAt, earnedAt);
		if (months < 3) {
			return FIRST_THREE_MONTHS_RATE;
		}
		if (months < 12) {
			return AFTER_THREE_MONTHS_RATE;
		}
		return AFTER_ONE_YEAR_RATE;
	}

	private List<AffiliateCommission> eligibleCommissions(TrackerUser affiliate) {
		return commissionRepository.findByAffiliate_IdAndStatusAndWithdrawalIsNullOrderByEarnedAtAsc(
				affiliate.getId(),
				AffiliateCommissionStatus.EARNED);
	}

	private BigDecimal availableAmount(List<AffiliateCommission> commissions) {
		return commissions.stream()
				.map(AffiliateCommission::getCommissionAmount)
				.map(this::normalizeMoney)
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private List<Tip> eligibleAffiliateTips(TrackerUser affiliate) {
		return tipRepository.findByWorker_IdAndStatusAndWithdrawalIsNullAndCreatedLessThanEqualOrderByCreatedDesc(
				affiliate.getId(),
				TipStatus.PAID,
				tipWithdrawableBefore());
	}

	private java.time.OffsetDateTime tipWithdrawableBefore() {
		return java.time.OffsetDateTime.now(clock).minusDays(tipHoldDays);
	}

	private BigDecimal availableTipNetAmount(List<Tip> tips) {
		return tips.stream()
				.map(tip -> tipNetAmount(tip.getTipAmount()))
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal tipNetAmount(BigDecimal tipAmount) {
		BigDecimal normalizedTipAmount = normalizeMoney(tipAmount);
		BigDecimal withdrawalFee = normalizedTipAmount
				.multiply(tipWithdrawalFeePercent)
				.divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
		return normalizedTipAmount.subtract(withdrawalFee).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private TrackerUser affiliate(String affiliateUsername) {
		TrackerUser affiliate = userRepository.findByUsername(normalizeRequired(affiliateUsername, "Username is required"))
				.orElseThrow(() -> new com.king_sparkon_tracker.backend.exception.ResourceNotFoundException("User not found: " + affiliateUsername));
		if (affiliate.getPrivilege() == null || affiliate.getPrivilege().getName() != PrivilegeRole.Affiliate) {
			throw new IllegalArgumentException("Only affiliates can manage affiliate payouts");
		}
		return affiliate;
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}
}
