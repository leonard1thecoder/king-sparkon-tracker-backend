package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountLedgerEntryResponse;
import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountSummaryResponse;
import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountTopUpRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryStatus;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;
import com.king_sparkon_tracker.backend.repository.BusinessAccountLedgerEntryRepository;

@Service
@Transactional
public class BusinessAccountService {

	private static final int MONEY_SCALE = 2;
	private static final String PROVIDER_STRIPE = "STRIPE";
	private static final String PROVIDER_KING_SPARKON = "KING_SPARKON";

	private final BusinessAccountLedgerEntryRepository ledgerRepository;
	private final TrackerUserService trackerUserService;
	private final StripeService stripeService;

	public BusinessAccountService(
			BusinessAccountLedgerEntryRepository ledgerRepository,
			TrackerUserService trackerUserService,
			StripeService stripeService) {
		this.ledgerRepository = ledgerRepository;
		this.trackerUserService = trackerUserService;
		this.stripeService = stripeService;
	}

	@Transactional(readOnly = true)
	public BusinessAccountSummaryResponse summary(String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return summaryForBusiness(business);
	}

	@Transactional(readOnly = true)
	public List<BusinessAccountLedgerEntryResponse> ledger(String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return ledgerForBusiness(business.getId());
	}

	@Transactional(readOnly = true)
	public List<BusinessAccountLedgerEntryResponse> ledgerForBusiness(Long businessId) {
		return ledgerRepository.findByBusiness_IdOrderByCreatedDateDesc(businessId)
				.stream()
				.map(BusinessAccountLedgerEntryResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<BusinessAccountLedgerEntryResponse> recentEntries(Long businessId, int limit) {
		return ledgerRepository.findByBusiness_IdOrderByCreatedDateDesc(businessId)
				.stream()
				.limit(Math.max(limit, 0))
				.map(BusinessAccountLedgerEntryResponse::from)
				.toList();
	}

	public BusinessAccountLedgerEntryResponse createTopUp(BusinessAccountTopUpRequest request, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		BigDecimal amount = normalizeMoney(request.amount());
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("Top-up amount must be greater than zero");
		}

		BigDecimal currentBalance = availableBalance(business.getId());
		var paymentLink = stripeService.createBusinessTopUpPaymentLink(
				business,
				amount,
				normalizeOptional(request.callbackUrl()),
				normalizeOptional(request.paymentMethod()));

		BusinessAccountLedgerEntry entry = new BusinessAccountLedgerEntry(
				business,
				BusinessAccountEntryType.TOP_UP,
				BusinessAccountEntryStatus.PENDING,
				amount,
				currentBalance,
				PROVIDER_STRIPE,
				paymentLink.stripeId(),
				paymentLink.paymentUrl(),
				"Business account top-up pending Stripe confirmation",
				actorUsername);
		return BusinessAccountLedgerEntryResponse.from(ledgerRepository.save(entry));
	}

	public BusinessAccountLedgerEntryResponse confirmTopUp(Long ledgerEntryId, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		BusinessAccountLedgerEntry entry = ledgerRepository.findById(ledgerEntryId)
				.orElseThrow(() -> new ResourceNotFoundException("Business account entry not found: " + ledgerEntryId));
		if (!entry.getBusiness().getId().equals(business.getId())) {
			throw new IllegalArgumentException("Business account entry does not belong to this owner");
		}
		if (entry.getEntryType() != BusinessAccountEntryType.TOP_UP) {
			throw new IllegalArgumentException("Only top-up entries can be confirmed");
		}
		if (entry.getStatus() == BusinessAccountEntryStatus.POSTED) {
			return BusinessAccountLedgerEntryResponse.from(entry);
		}

		ledgerRepository.lockPostedEntries(business.getId(), BusinessAccountEntryStatus.POSTED);
		BigDecimal newBalance = availableBalance(business.getId()).add(entry.getAmount());
		entry.markPosted(normalizeMoney(newBalance));
		return BusinessAccountLedgerEntryResponse.from(ledgerRepository.save(entry));
	}

	public BusinessAccountLedgerEntry debitPromotion(
			Business business,
			BigDecimal amount,
			BusinessAccountEntryType entryType,
			String description,
			String actorUsername) {
		if (entryType != BusinessAccountEntryType.PROMOTION_DEBIT
				&& entryType != BusinessAccountEntryType.TICKET_PROMOTION_DEBIT) {
			throw new IllegalArgumentException("Unsupported business account debit type: " + entryType);
		}
		return postDebit(business, amount, entryType, null, description, actorUsername, true);
	}

	public BusinessAccountLedgerEntry postRevenueCreditIfAbsent(
			Business business,
			BigDecimal amount,
			BusinessAccountEntryType entryType,
			String providerReference,
			String description) {
		if (entryType != BusinessAccountEntryType.PRODUCT_SALE_CREDIT
				&& entryType != BusinessAccountEntryType.TICKET_SALE_CREDIT
				&& entryType != BusinessAccountEntryType.TIP_CREDIT) {
			throw new IllegalArgumentException("Unsupported revenue credit type: " + entryType);
		}
		return postSignedEntryIfAbsent(
				business,
				normalizePositive(amount, "Revenue amount must be greater than zero"),
				entryType,
				required(providerReference, "Revenue provider reference is required"),
				description,
				"SYSTEM_RECONCILIATION");
	}

	public BusinessAccountLedgerEntry postHistoricalWithdrawalIfAbsent(
			Business business,
			BigDecimal grossAmount,
			String providerReference,
			String description) {
		BigDecimal debit = normalizePositive(grossAmount, "Historical withdrawal amount must be greater than zero").negate();
		return postSignedEntryIfAbsent(
				business,
				debit,
				BusinessAccountEntryType.OWNER_WITHDRAWAL_DEBIT,
				required(providerReference, "Historical withdrawal reference is required"),
				description,
				"LEGACY_WITHDRAWAL_RECONCILIATION");
	}

	public BusinessAccountLedgerEntry postOwnerWithdrawal(
			Business business,
			BigDecimal amount,
			String providerReference,
			String description,
			String actorUsername) {
		return postDebit(
				business,
				amount,
				BusinessAccountEntryType.OWNER_WITHDRAWAL_DEBIT,
				required(providerReference, "Withdrawal reference is required"),
				description,
				actorUsername,
				true);
	}

	public BusinessAccountLedgerEntry postOwnerWithdrawalReversalIfAbsent(
			Business business,
			BigDecimal amount,
			String providerReference,
			String description) {
		return postSignedEntryIfAbsent(
				business,
				normalizePositive(amount, "Withdrawal reversal amount must be greater than zero"),
				BusinessAccountEntryType.OWNER_WITHDRAWAL_REVERSAL_CREDIT,
				required(providerReference, "Withdrawal reversal reference is required"),
				description,
				"PAYPAL_PAYOUT_RECONCILIATION");
	}

	private BusinessAccountLedgerEntry postDebit(
			Business business,
			BigDecimal amount,
			BusinessAccountEntryType entryType,
			String providerReference,
			String description,
			String actorUsername,
			boolean requireFunds) {
		requireBusiness(business);
		BigDecimal debitAmount = normalizePositive(amount, "Debit amount must be greater than zero");
		ledgerRepository.lockPostedEntries(business.getId(), BusinessAccountEntryStatus.POSTED);
		BigDecimal currentBalance = availableBalance(business.getId());
		if (requireFunds && currentBalance.compareTo(debitAmount) < 0) {
			throw new IllegalStateException("Business account balance is too low for this debit");
		}
		BigDecimal newBalance = normalizeMoney(currentBalance.subtract(debitAmount));
		BusinessAccountLedgerEntry entry = new BusinessAccountLedgerEntry(
				business,
				entryType,
				BusinessAccountEntryStatus.POSTED,
				debitAmount.negate(),
				newBalance,
				PROVIDER_KING_SPARKON,
				providerReference,
				null,
				normalizeOptional(description),
				required(actorUsername, "Debit actor is required"));
		return ledgerRepository.save(entry);
	}

	private BusinessAccountLedgerEntry postSignedEntryIfAbsent(
			Business business,
			BigDecimal amount,
			BusinessAccountEntryType entryType,
			String providerReference,
			String description,
			String actorUsername) {
		requireBusiness(business);
		ledgerRepository.lockPostedEntries(business.getId(), BusinessAccountEntryStatus.POSTED);
		var existing = ledgerRepository.findByBusiness_IdAndEntryTypeAndProviderReference(
				business.getId(), entryType, providerReference);
		if (existing.isPresent()) {
			return existing.get();
		}
		BigDecimal newBalance = normalizeMoney(availableBalance(business.getId()).add(amount));
		BusinessAccountLedgerEntry entry = new BusinessAccountLedgerEntry(
				business,
				entryType,
				BusinessAccountEntryStatus.POSTED,
				normalizeMoney(amount),
				newBalance,
				PROVIDER_KING_SPARKON,
				providerReference,
				null,
				normalizeOptional(description),
				actorUsername);
		return ledgerRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public BigDecimal availableBalance(Long businessId) {
		BigDecimal balance = ledgerRepository.sumAmountByBusinessIdAndStatus(
				businessId,
				BusinessAccountEntryStatus.POSTED);
		return normalizeMoney(balance == null ? BigDecimal.ZERO : balance);
	}

	@Transactional(readOnly = true)
	public BigDecimal sumByTypes(Long businessId, Collection<BusinessAccountEntryType> entryTypes) {
		if (entryTypes == null || entryTypes.isEmpty()) {
			return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		}
		BigDecimal amount = ledgerRepository.sumAmountByBusinessIdAndStatusAndEntryTypeIn(
				businessId,
				BusinessAccountEntryStatus.POSTED,
				entryTypes);
		return normalizeMoney(amount == null ? BigDecimal.ZERO : amount);
	}

	private BusinessAccountSummaryResponse summaryForBusiness(Business business) {
		return new BusinessAccountSummaryResponse(
				business.getId(),
				business.getName(),
				availableBalance(business.getId()),
				recentEntries(business.getId(), 10));
	}

	private void requireBusiness(Business business) {
		if (business == null || business.getId() == null) {
			throw new IllegalArgumentException("Business account is required");
		}
	}

	private BigDecimal normalizePositive(BigDecimal amount, String message) {
		BigDecimal normalized = normalizeMoney(amount);
		if (normalized.signum() <= 0) {
			throw new IllegalArgumentException(message);
		}
		return normalized;
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null
				? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}
}
