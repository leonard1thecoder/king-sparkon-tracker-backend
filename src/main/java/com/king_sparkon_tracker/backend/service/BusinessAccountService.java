package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
		return ledgerRepository.findByBusiness_IdOrderByCreatedDateDesc(business.getId())
				.stream()
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
		if (business == null || business.getId() == null) {
			throw new IllegalArgumentException("Business account is required");
		}
		if (entryType != BusinessAccountEntryType.PROMOTION_DEBIT && entryType != BusinessAccountEntryType.TICKET_PROMOTION_DEBIT) {
			throw new IllegalArgumentException("Unsupported business account debit type: " + entryType);
		}

		BigDecimal debitAmount = normalizeMoney(amount);
		if (debitAmount.signum() <= 0) {
			throw new IllegalArgumentException("Promotion amount must be greater than zero");
		}

		ledgerRepository.lockPostedEntries(business.getId(), BusinessAccountEntryStatus.POSTED);
		BigDecimal currentBalance = availableBalance(business.getId());
		if (currentBalance.compareTo(debitAmount) < 0) {
			throw new IllegalStateException("Business account balance is too low for this promotion. Top up the account first.");
		}

		BigDecimal newBalance = normalizeMoney(currentBalance.subtract(debitAmount));
		BusinessAccountLedgerEntry entry = new BusinessAccountLedgerEntry(
				business,
				entryType,
				BusinessAccountEntryStatus.POSTED,
				debitAmount.negate(),
				newBalance,
				null,
				null,
				null,
				normalizeOptional(description),
				actorUsername);
		return ledgerRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public BigDecimal availableBalance(Long businessId) {
		BigDecimal balance = ledgerRepository.sumAmountByBusinessIdAndStatus(businessId, BusinessAccountEntryStatus.POSTED);
		return normalizeMoney(balance == null ? BigDecimal.ZERO : balance);
	}

	private BusinessAccountSummaryResponse summaryForBusiness(Business business) {
		List<BusinessAccountLedgerEntryResponse> entries = ledgerRepository.findByBusiness_IdOrderByCreatedDateDesc(business.getId())
				.stream()
				.limit(10)
				.map(BusinessAccountLedgerEntryResponse::from)
				.toList();
		return new BusinessAccountSummaryResponse(
				business.getId(),
				business.getName(),
				availableBalance(business.getId()),
				entries);
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
