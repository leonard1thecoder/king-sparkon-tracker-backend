package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.OwnerPayoutAccount;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.OwnerPayoutAccountRepository;
import com.king_sparkon_tracker.backend.repository.TransactionWithdrawalRepository;

@Service
public class TransactionWithdrawalService {

	private static final int MONEY_SCALE = 2;

	private final InventoryTransactionRepository transactionRepository;
	private final OwnerPayoutAccountRepository payoutAccountRepository;
	private final TransactionWithdrawalRepository withdrawalRepository;
	private final PriceLocalizationService priceLocalizationService;
	private final NotificationService notificationService;
	private final TrackerUserService trackerUserService;
	private final BigDecimal minimumWithdrawalAmount;
	private final int holdDays;
	private final BigDecimal websiteWithdrawalFeePercent;
	private final String defaultOnboardingUrl;

	public TransactionWithdrawalService(
			InventoryTransactionRepository transactionRepository,
			OwnerPayoutAccountRepository payoutAccountRepository,
			TransactionWithdrawalRepository withdrawalRepository,
			PriceLocalizationService priceLocalizationService,
			NotificationService notificationService,
			TrackerUserService trackerUserService,
			@Value("${app.transactions.withdrawal-minimum-zar:1000}") BigDecimal minimumWithdrawalAmount,
			@Value("${app.transactions.withdrawal-hold-days:7}") int holdDays,
			@Value("${app.transactions.website-withdrawal-fee-percent:6.5}") BigDecimal websiteWithdrawalFeePercent,
			@Value("${app.transactions.paypal-onboarding-url:http://localhost:3000/dashboard/owner/transactions/paypal/onboarding}") String defaultOnboardingUrl) {
		this.transactionRepository = transactionRepository;
		this.payoutAccountRepository = payoutAccountRepository;
		this.withdrawalRepository = withdrawalRepository;
		this.priceLocalizationService = priceLocalizationService;
		this.notificationService = notificationService;
		this.trackerUserService = trackerUserService;
		this.minimumWithdrawalAmount = normalizeMoney(minimumWithdrawalAmount);
		this.holdDays = holdDays;
		this.websiteWithdrawalFeePercent = websiteWithdrawalFeePercent.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		this.defaultOnboardingUrl = defaultOnboardingUrl;
	}

	@Transactional
	public OwnerPayPalAccountResponse onboardPayPalAccount(
			OwnerPayPalAccountOnboardingRequest request,
			String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		Business business = business(owner);
		String paypalEmail = EmailAddressNormalizer.normalizeRequired(
				request.paypalEmail(),
				"Owner PayPal email must be a valid email address");
		String onboardingToken = UUID.randomUUID().toString();
		String onboardingUrl = onboardingUrl(request.callbackUrl(), business.getId(), onboardingToken);

		OwnerPayoutAccount account = payoutAccountRepository.findByBusinessId(business.getId())
				.map(existing -> {
					existing.update(owner.getId(), paypalEmail, onboardingToken, onboardingUrl);
					return existing;
				})
				.orElseGet(() -> new OwnerPayoutAccount(
						owner.getId(),
						business.getId(),
						paypalEmail,
						onboardingToken,
						onboardingUrl));

		return OwnerPayPalAccountResponse.from(payoutAccountRepository.save(account));
	}

	@Transactional(readOnly = true)
	public TransactionWithdrawalEligibilityResponse eligibility(String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		Business business = business(owner);
		LocalDateTime availableBefore = availableBefore();
		List<InventoryTransaction> transactions = eligibleTransactions(business.getId(), availableBefore);
		BigDecimal grossAmount = grossAmount(transactions);
		BigDecimal feeAmount = feeAmount(grossAmount);
		BigDecimal availableAmount = netAmount(grossAmount, feeAmount);
		boolean paypalAccountReady = payoutAccountRepository.findByBusinessId(business.getId()).isPresent();
		boolean canWithdraw = paypalAccountReady
				&& availableAmount.compareTo(minimumWithdrawalAmount) >= 0;

		return new TransactionWithdrawalEligibilityResponse(
				owner.getId(),
				business.getId(),
				grossAmount,
				priceLocalizationService.localize(grossAmount, owner),
				feeAmount,
				priceLocalizationService.localize(feeAmount, owner),
				websiteWithdrawalFeePercent,
				availableAmount,
				priceLocalizationService.localize(availableAmount, owner),
				priceLocalizationService.localize(minimumWithdrawalAmount, owner),
				transactions.size(),
				holdDays,
				paypalAccountReady,
				canWithdraw,
				availableBefore);
	}

	@Transactional
	public TransactionWithdrawalResponse requestWithdrawal(
			TransactionWithdrawalRequest request,
			String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		Business business = business(owner);
		OwnerPayoutAccount account = payoutAccountRepository.findByBusinessId(business.getId())
				.orElseThrow(() -> new IllegalArgumentException("Owner PayPal payout account onboarding is required before transaction withdrawal"));

		LocalDateTime availableBefore = availableBefore();
		List<InventoryTransaction> eligibleTransactions = eligibleTransactions(business.getId(), request, availableBefore);
		BigDecimal grossAmount = grossAmount(eligibleTransactions);
		BigDecimal feeAmount = feeAmount(grossAmount);
		BigDecimal netAmount = netAmount(grossAmount, feeAmount);
		if (netAmount.compareTo(minimumWithdrawalAmount) < 0) {
			throw new IllegalArgumentException("Available website payment balance must be at least R1000.00 before withdrawal");
		}

		TransactionWithdrawal withdrawal = withdrawalRepository.save(new TransactionWithdrawal(
				owner.getId(),
				business.getId(),
				grossAmount,
				feeAmount,
				websiteWithdrawalFeePercent,
				netAmount,
				"ZAR",
				eligibleTransactions.size(),
				account.getPaypalEmail()));

		for (InventoryTransaction transaction : eligibleTransactions) {
			transaction.assignTransactionWithdrawal(withdrawal.getId());
		}
		transactionRepository.saveAll(eligibleTransactions);

		notificationService.logTransactionWithdrawalRequested(withdrawal);
		return response(withdrawal, owner);
	}

	@Transactional(readOnly = true)
	public List<TransactionWithdrawalResponse> getWithdrawals(String ownerUsername) {
		TrackerUser owner = owner(ownerUsername);
		Business business = business(owner);
		return withdrawalRepository.findByBusinessIdOrderByRequestedAtDesc(business.getId())
				.stream()
				.map(withdrawal -> response(withdrawal, owner))
				.toList();
	}

	private List<InventoryTransaction> eligibleTransactions(
			Long businessId,
			TransactionWithdrawalRequest request,
			LocalDateTime availableBefore) {
		List<Long> transactionIds = request == null ? List.of() : normalizeTransactionIds(request.transactionIds());
		if (transactionIds.isEmpty()) {
			return eligibleTransactions(businessId, availableBefore);
		}

		List<InventoryTransaction> transactions = transactionRepository.findEligibleWebsitePaymentWithdrawalsByIds(
				businessId,
				transactionIds,
				TransactionType.SELL,
				TransactionPaymentType.WEBSITE_PAYMENT,
				TransactionPaymentStatus.PAID,
				availableBefore);
		if (transactions.size() != transactionIds.size()) {
			throw new IllegalArgumentException("One or more website payment transactions are not eligible for withdrawal");
		}
		return transactions;
	}

	private List<InventoryTransaction> eligibleTransactions(Long businessId, LocalDateTime availableBefore) {
		return transactionRepository
				.findByBusiness_IdAndTypeAndPaymentTypeAndPaymentStatusAndTransactionWithdrawalIdIsNullAndDateLessThanEqualOrderByDateAsc(
						businessId,
						TransactionType.SELL,
						TransactionPaymentType.WEBSITE_PAYMENT,
						TransactionPaymentStatus.PAID,
						availableBefore);
	}

	private List<Long> normalizeTransactionIds(List<Long> transactionIds) {
		if (transactionIds == null || transactionIds.isEmpty()) {
			return List.of();
		}
		Set<Long> uniqueIds = new LinkedHashSet<>();
		for (Long transactionId : transactionIds) {
			if (transactionId == null || transactionId <= 0) {
				throw new IllegalArgumentException("Transaction ids must be positive");
			}
			uniqueIds.add(transactionId);
		}
		return List.copyOf(uniqueIds);
	}

	private TransactionWithdrawalResponse response(TransactionWithdrawal withdrawal, TrackerUser owner) {
		return TransactionWithdrawalResponse.from(
				withdrawal,
				priceLocalizationService.localize(withdrawal.getGrossAmount(), owner),
				priceLocalizationService.localize(withdrawal.getFeeAmount(), owner),
				priceLocalizationService.localize(withdrawal.getAmount(), owner));
	}

	private BigDecimal grossAmount(List<InventoryTransaction> transactions) {
		return transactions.stream()
				.map(InventoryTransaction::getTotalAmount)
				.map(this::normalizeMoney)
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal feeAmount(BigDecimal grossAmount) {
		return normalizeMoney(grossAmount)
				.multiply(websiteWithdrawalFeePercent)
				.divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal netAmount(BigDecimal grossAmount, BigDecimal feeAmount) {
		return normalizeMoney(grossAmount)
				.subtract(normalizeMoney(feeAmount))
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private LocalDateTime availableBefore() {
		return LocalDateTime.now().minusDays(holdDays);
	}

	private String onboardingUrl(String callbackUrl, Long businessId, String onboardingToken) {
		String baseUrl = callbackUrl == null || callbackUrl.isBlank()
				? defaultOnboardingUrl
				: callbackUrl.trim();

		return UriComponentsBuilder.fromUriString(baseUrl)
				.queryParam("businessId", businessId)
				.queryParam("token", URLEncoder.encode(onboardingToken, StandardCharsets.UTF_8))
				.build(true)
				.toUriString();
	}

	private TrackerUser owner(String ownerUsername) {
		TrackerUser owner = trackerUserService.getUserByUsername(ownerUsername);
		if (owner.getPrivilege() == null || owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can manage transaction withdrawals");
		}
		return owner;
	}

	private Business business(TrackerUser owner) {
		if (owner.getBusiness() == null) {
			throw new IllegalArgumentException("Owner is not linked to a business");
		}
		return owner.getBusiness();
	}
}
