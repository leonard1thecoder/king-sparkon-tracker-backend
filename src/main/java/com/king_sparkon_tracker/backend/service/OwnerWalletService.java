package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.OwnerWalletDtos.OwnerWalletSummaryResponse;
import com.king_sparkon_tracker.backend.dto.OwnerWalletDtos.OwnerWithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.OwnerWalletDtos.OwnerWithdrawalResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;
import com.king_sparkon_tracker.backend.model.BusinessWithdrawal;
import com.king_sparkon_tracker.backend.model.BusinessWithdrawalStatus;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;
import com.king_sparkon_tracker.backend.repository.BusinessWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.repository.TipWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.TransactionWithdrawalRepository;
import com.king_sparkon_tracker.backend.service.PayPalPayoutService.PayoutQuote;
import com.king_sparkon_tracker.backend.service.PayPalPayoutService.PayoutSubmission;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketWithdrawal;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketPaymentRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketWithdrawalRepository;

@Service
@Transactional
public class OwnerWalletService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OwnerWalletService.class);
	private static final int MONEY_SCALE = 2;
	private static final String CURRENCY = "ZAR";
	private static final String PAYPAL = "PAYPAL";

	private final BusinessAccountService businessAccountService;
	private final TrackerUserService trackerUserService;
	private final InventoryTransactionRepository transactionRepository;
	private final TipRepository tipRepository;
	private final TicketEventRepository ticketEventRepository;
	private final TicketPaymentRepository ticketPaymentRepository;
	private final TransactionWithdrawalRepository transactionWithdrawalRepository;
	private final TipWithdrawalRepository tipWithdrawalRepository;
	private final TicketWithdrawalRepository ticketWithdrawalRepository;
	private final BusinessWithdrawalRepository businessWithdrawalRepository;
	private final PayPalPayoutService payPalPayoutService;
	private final BigDecimal minimumWithdrawalAmount;

	public OwnerWalletService(
			BusinessAccountService businessAccountService,
			TrackerUserService trackerUserService,
			InventoryTransactionRepository transactionRepository,
			TipRepository tipRepository,
			TicketEventRepository ticketEventRepository,
			TicketPaymentRepository ticketPaymentRepository,
			TransactionWithdrawalRepository transactionWithdrawalRepository,
			TipWithdrawalRepository tipWithdrawalRepository,
			TicketWithdrawalRepository ticketWithdrawalRepository,
			BusinessWithdrawalRepository businessWithdrawalRepository,
			PayPalPayoutService payPalPayoutService,
			@Value("${app.business-account.withdrawal-minimum-zar:100}") BigDecimal minimumWithdrawalAmount) {
		this.businessAccountService = businessAccountService;
		this.trackerUserService = trackerUserService;
		this.transactionRepository = transactionRepository;
		this.tipRepository = tipRepository;
		this.ticketEventRepository = ticketEventRepository;
		this.ticketPaymentRepository = ticketPaymentRepository;
		this.transactionWithdrawalRepository = transactionWithdrawalRepository;
		this.tipWithdrawalRepository = tipWithdrawalRepository;
		this.ticketWithdrawalRepository = ticketWithdrawalRepository;
		this.businessWithdrawalRepository = businessWithdrawalRepository;
		this.payPalPayoutService = payPalPayoutService;
		this.minimumWithdrawalAmount = money(minimumWithdrawalAmount);
	}

	public OwnerWalletSummaryResponse summary(String actorUsername) {
		TrackerUser owner = owner(actorUsername);
		Business business = business(owner);
		reconcile(business, owner);

		BigDecimal productRevenue = positiveSum(business.getId(), BusinessAccountEntryType.PRODUCT_SALE_CREDIT);
		BigDecimal ticketRevenue = positiveSum(business.getId(), BusinessAccountEntryType.TICKET_SALE_CREDIT);
		BigDecimal tipRevenue = positiveSum(business.getId(), BusinessAccountEntryType.TIP_CREDIT);
		BigDecimal topUps = positiveSum(business.getId(), BusinessAccountEntryType.TOP_UP);
		BigDecimal promotionSpend = negativeMagnitude(business.getId(), EnumSet.of(
				BusinessAccountEntryType.PROMOTION_DEBIT,
				BusinessAccountEntryType.TICKET_PROMOTION_DEBIT));
		BigDecimal withdrawalDebits = negativeMagnitude(
				business.getId(),
				EnumSet.of(BusinessAccountEntryType.OWNER_WITHDRAWAL_DEBIT));
		BigDecimal withdrawalReversals = positiveSum(
				business.getId(),
				BusinessAccountEntryType.OWNER_WITHDRAWAL_REVERSAL_CREDIT);
		BigDecimal withdrawn = withdrawalDebits.subtract(withdrawalReversals)
				.max(BigDecimal.ZERO.setScale(MONEY_SCALE));
		BigDecimal balance = businessAccountService.availableBalance(business.getId())
				.max(BigDecimal.ZERO.setScale(MONEY_SCALE));
		long pendingWithdrawals = businessWithdrawalRepository.countByBusiness_IdAndStatusIn(
				business.getId(),
				List.of(BusinessWithdrawalStatus.REQUESTED, BusinessWithdrawalStatus.PROCESSING));

		return new OwnerWalletSummaryResponse(
				business.getId(),
				business.getName(),
				balance,
				minimumWithdrawalAmount,
				productRevenue,
				ticketRevenue,
				tipRevenue,
				topUps,
				promotionSpend,
				withdrawn,
				pendingWithdrawals,
				businessAccountService.recentEntries(business.getId(), 12),
				PAYPAL,
				payPalPayoutService.payoutCurrency(),
				payPalPayoutService.zarPerPayoutUnit(),
				payPalPayoutService.isConfigured());
	}

	public List<OwnerWithdrawalResponse> withdrawals(String actorUsername) {
		TrackerUser owner = owner(actorUsername);
		Business business = business(owner);
		reconcile(business, owner);

		List<OwnerWithdrawalResponse> responses = new ArrayList<>();
		businessWithdrawalRepository.findByBusiness_IdOrderByRequestedAtDesc(business.getId())
				.forEach(withdrawal -> responses.add(fromUnified(withdrawal)));
		transactionWithdrawalRepository.findByBusinessIdOrderByRequestedAtDesc(business.getId())
				.forEach(withdrawal -> responses.add(fromProduct(withdrawal)));
		tipWithdrawalRepository.findByOwner_Business_IdOrderByRequestedAtDesc(business.getId())
				.forEach(withdrawal -> responses.add(fromTip(withdrawal, legacyTipGross(withdrawal))));
		legacyTicketWithdrawals(owner).forEach(withdrawal -> responses.add(fromTicket(business.getId(), withdrawal)));
		responses.sort(Comparator.comparing(OwnerWithdrawalResponse::requestedAt, Comparator.nullsLast(Comparator.reverseOrder())));
		return List.copyOf(responses);
	}

	public OwnerWithdrawalResponse requestWithdrawal(OwnerWithdrawalRequest request, String actorUsername) {
		TrackerUser owner = owner(actorUsername);
		Business business = business(owner);
		reconcile(business, owner);

		BigDecimal amount = money(request.amount());
		if (amount.compareTo(minimumWithdrawalAmount) < 0) {
			throw new IllegalArgumentException("Owner withdrawal amount must be at least " + CURRENCY + " " + minimumWithdrawalAmount);
		}
		BigDecimal availableBalance = businessAccountService.availableBalance(business.getId());
		if (availableBalance.compareTo(amount) < 0) {
			throw new IllegalStateException("Withdrawal amount is greater than the available King Sparkon balance");
		}

		String payoutMethod = required(request.payoutMethod(), "Payout method is required").toUpperCase(Locale.ROOT);
		if (!PAYPAL.equals(payoutMethod)) {
			throw new IllegalArgumentException("Owner withdrawals are PayPal-only");
		}
		String payoutDestination = optional(request.payoutDestination());
		if (payoutDestination == null) payoutDestination = optional(owner.getEmailAddress());
		payoutDestination = paypalEmail(payoutDestination);

		PayoutQuote quote = payPalPayoutService.quote(amount);
		BusinessWithdrawal withdrawal = businessWithdrawalRepository.saveAndFlush(new BusinessWithdrawal(
				business,
				owner.getId(),
				amount,
				PAYPAL,
				payoutDestination,
				optional(request.notes())));

		PayoutSubmission payout = payPalPayoutService.submitWithdrawal(
				withdrawal.getId(),
				amount,
				payoutDestination);
		withdrawal.markPayoutSubmitted(
				PAYPAL,
				payout.payoutBatchId(),
				payout.batchStatus(),
				payout.quote().payoutAmount(),
				payout.quote().payoutCurrency());
		if (withdrawal.getStatus() == BusinessWithdrawalStatus.FAILED) {
			throw new IllegalStateException("PayPal rejected the payout batch before the balance was debited");
		}

		BusinessAccountLedgerEntry ledgerEntry = businessAccountService.postOwnerWithdrawal(
				business,
				amount,
				"PAYPAL-PAYOUT:" + payout.payoutBatchId(),
				"Owner PayPal withdrawal " + payout.payoutBatchId()
						+ " · " + quote.payoutAmount() + " " + quote.payoutCurrency(),
				actorUsername);
		withdrawal.attachLedgerEntry(ledgerEntry.getId());
		return fromUnified(businessWithdrawalRepository.save(withdrawal));
	}

	private void reconcile(Business business, TrackerUser owner) {
		reconcileProductRevenue(business);
		reconcileTicketRevenue(business, owner);
		reconcileTipRevenue(business);
		reconcileLegacyProductWithdrawals(business);
		reconcileLegacyTipWithdrawals(business);
		reconcileLegacyTicketWithdrawals(business, owner);
		refreshPayPalWithdrawals(business);
	}

	private void refreshPayPalWithdrawals(Business business) {
		if (!payPalPayoutService.isConfigured()) return;
		List<BusinessWithdrawal> active = businessWithdrawalRepository
				.findByBusiness_IdAndStatusInOrderByRequestedAtDesc(
						business.getId(),
						List.of(BusinessWithdrawalStatus.REQUESTED, BusinessWithdrawalStatus.PROCESSING));
		for (BusinessWithdrawal withdrawal : active) {
			if (!PAYPAL.equalsIgnoreCase(withdrawal.getProvider())
					|| !StringUtils.hasText(withdrawal.getProviderBatchId())) {
				continue;
			}
			try {
				String providerStatus = payPalPayoutService.getBatchStatus(withdrawal.getProviderBatchId());
				withdrawal.applyProviderStatus(providerStatus);
				businessWithdrawalRepository.save(withdrawal);
				if (withdrawal.getStatus() == BusinessWithdrawalStatus.FAILED
						&& withdrawal.getLedgerEntryId() != null) {
					businessAccountService.postOwnerWithdrawalReversalIfAbsent(
							business,
							withdrawal.getAmount(),
							"PAYPAL-PAYOUT-REVERSAL:" + withdrawal.getId(),
							"PayPal payout " + withdrawal.getProviderBatchId() + " failed; owner balance restored");
				}
			} catch (RuntimeException exception) {
				LOGGER.warn(
						"Could not refresh PayPal payout status for withdrawal {} and batch {}",
						withdrawal.getId(),
						withdrawal.getProviderBatchId(),
						exception);
			}
		}
	}

	private void reconcileProductRevenue(Business business) {
		transactionRepository.findByBusiness_Id(business.getId(), Pageable.unpaged()).getContent().stream()
				.filter(transaction -> transaction.getType() == TransactionType.SELL)
				.filter(transaction -> transaction.getPaymentType() == TransactionPaymentType.WEBSITE_PAYMENT)
				.filter(transaction -> transaction.getPaymentStatus() == TransactionPaymentStatus.PAID)
				.forEach(transaction -> businessAccountService.postRevenueCreditIfAbsent(
						business,
						transaction.getTotalAmount(),
						BusinessAccountEntryType.PRODUCT_SALE_CREDIT,
						"PRODUCT-TRANSACTION:" + transaction.getId(),
						"Paid King Sparkon product order #" + transaction.getId()));
	}

	private void reconcileTicketRevenue(Business business, TrackerUser owner) {
		List<String> eventIds = ownerEvents(owner).stream().map(TicketEvent::getId).distinct().toList();
		if (eventIds.isEmpty()) return;
		ticketPaymentRepository.findByEventIdIn(eventIds).stream()
				.filter(payment -> payment.getStatus() == TicketPaymentStatus.SUCCESS)
				.forEach(payment -> businessAccountService.postRevenueCreditIfAbsent(
						business,
						payment.getSubtotalAmount(),
						BusinessAccountEntryType.TICKET_SALE_CREDIT,
						"TICKET-PAYMENT:" + payment.getId(),
						"Successful ticket payment " + payment.getId() + " · " + payment.getQuantity() + " tickets"));
	}

	private void reconcileTipRevenue(Business business) {
		tipRepository.findByWorker_Business_IdAndStatusOrderByCreatedDesc(business.getId(), TipStatus.PAID)
				.forEach(tip -> businessAccountService.postRevenueCreditIfAbsent(
						business,
						tip.getTipAmount(),
						BusinessAccountEntryType.TIP_CREDIT,
						"TIP:" + tip.getId(),
						"Paid tip for worker #" + tip.getWorkerId()));
	}

	private void reconcileLegacyProductWithdrawals(Business business) {
		transactionWithdrawalRepository.findByBusinessIdOrderByRequestedAtDesc(business.getId())
				.forEach(withdrawal -> businessAccountService.postHistoricalWithdrawalIfAbsent(
						business,
						withdrawal.getGrossAmount(),
						"LEGACY-PRODUCT-WITHDRAWAL:" + withdrawal.getId(),
						"Imported product withdrawal · net " + money(withdrawal.getAmount())));
	}

	private void reconcileLegacyTipWithdrawals(Business business) {
		tipWithdrawalRepository.findByOwner_Business_IdOrderByRequestedAtDesc(business.getId())
				.forEach(withdrawal -> businessAccountService.postHistoricalWithdrawalIfAbsent(
						business,
						legacyTipGross(withdrawal),
						"LEGACY-TIP-WITHDRAWAL:" + withdrawal.getId(),
						"Imported tip withdrawal for worker #" + withdrawal.getWorkerId()));
	}

	private void reconcileLegacyTicketWithdrawals(Business business, TrackerUser owner) {
		legacyTicketWithdrawals(owner).forEach(withdrawal -> businessAccountService.postHistoricalWithdrawalIfAbsent(
				business,
				withdrawal.getGrossAmount(),
				"LEGACY-TICKET-WITHDRAWAL:" + withdrawal.getId(),
				"Imported ticket withdrawal " + withdrawal.getId()));
	}

	private List<TicketEvent> ownerEvents(TrackerUser owner) {
		Map<String, TicketEvent> events = new LinkedHashMap<>();
		for (String ownerKey : ownerKeys(owner)) {
			ticketEventRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerKey)
					.forEach(event -> events.put(event.getId(), event));
		}
		return List.copyOf(events.values());
	}

	private List<TicketWithdrawal> legacyTicketWithdrawals(TrackerUser owner) {
		Map<String, TicketWithdrawal> withdrawals = new LinkedHashMap<>();
		for (String ownerKey : ownerKeys(owner)) {
			ticketWithdrawalRepository.findByOwnerIdOrderByRequestedAtDesc(ownerKey)
					.forEach(withdrawal -> withdrawals.put(withdrawal.getId(), withdrawal));
		}
		return List.copyOf(withdrawals.values());
	}

	private List<String> ownerKeys(TrackerUser owner) {
		return List.of(String.valueOf(owner.getId()), owner.getUsername()).stream().distinct().toList();
	}

	private BigDecimal legacyTipGross(TipWithdrawal withdrawal) {
		BigDecimal gross = tipRepository.findByWithdrawal_IdOrderByCreatedDesc(withdrawal.getId()).stream()
				.map(Tip::getTipAmount)
				.map(this::money)
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add);
		return gross.signum() > 0 ? gross : money(withdrawal.getAmount());
	}

	private BigDecimal positiveSum(Long businessId, BusinessAccountEntryType entryType) {
		return businessAccountService.sumByTypes(businessId, List.of(entryType))
				.max(BigDecimal.ZERO.setScale(MONEY_SCALE));
	}

	private BigDecimal negativeMagnitude(Long businessId, EnumSet<BusinessAccountEntryType> entryTypes) {
		return businessAccountService.sumByTypes(businessId, entryTypes)
				.min(BigDecimal.ZERO.setScale(MONEY_SCALE))
				.abs();
	}

	private OwnerWithdrawalResponse fromUnified(BusinessWithdrawal withdrawal) {
		return new OwnerWithdrawalResponse(
				"unified-" + withdrawal.getId(),
				"UNIFIED",
				withdrawal.getBusiness().getId(),
				money(withdrawal.getAmount()),
				BigDecimal.ZERO.setScale(MONEY_SCALE),
				money(withdrawal.getAmount()),
				CURRENCY,
				withdrawal.getStatus().name(),
				withdrawal.getPayoutMethod(),
				withdrawal.getPayoutDestination(),
				withdrawal.getNotes(),
				withdrawal.getRequestedAt(),
				withdrawal.getProcessedAt(),
				withdrawal.getProvider(),
				withdrawal.getProviderBatchId(),
				withdrawal.getProviderStatus(),
				money(withdrawal.getPayoutAmount()),
				withdrawal.getPayoutCurrency());
	}

	private OwnerWithdrawalResponse fromProduct(TransactionWithdrawal withdrawal) {
		return new OwnerWithdrawalResponse(
				"product-" + withdrawal.getId(),
				"PRODUCT",
				withdrawal.getBusinessId(),
				money(withdrawal.getGrossAmount()),
				money(withdrawal.getFeeAmount()),
				money(withdrawal.getAmount()),
				withdrawal.getCurrency(),
				withdrawal.getStatus().name(),
				PAYPAL,
				withdrawal.getPaypalEmail(),
				"Legacy product-payment withdrawal",
				withdrawal.getRequestedAt(),
				null,
				PAYPAL,
				null,
				withdrawal.getStatus().name(),
				money(withdrawal.getAmount()),
				withdrawal.getCurrency());
	}

	private OwnerWithdrawalResponse fromTip(TipWithdrawal withdrawal, BigDecimal grossAmount) {
		BigDecimal netAmount = money(withdrawal.getAmount());
		return new OwnerWithdrawalResponse(
				"tip-" + withdrawal.getId(),
				"TIP",
				withdrawal.getOwner().getBusiness().getId(),
				money(grossAmount),
				money(grossAmount).subtract(netAmount).max(BigDecimal.ZERO.setScale(MONEY_SCALE)),
				netAmount,
				withdrawal.getCurrency(),
				withdrawal.getStatus().name(),
				PAYPAL,
				withdrawal.getPaypalEmail(),
				"Legacy worker-tip withdrawal",
				withdrawal.getRequestedAt(),
				null,
				PAYPAL,
				null,
				withdrawal.getStatus().name(),
				netAmount,
				withdrawal.getCurrency());
	}

	private OwnerWithdrawalResponse fromTicket(Long businessId, TicketWithdrawal withdrawal) {
		return new OwnerWithdrawalResponse(
				"ticket-" + withdrawal.getId(),
				"TICKET",
				businessId,
				money(withdrawal.getGrossAmount()),
				money(withdrawal.getServiceFeeAmount()),
				money(withdrawal.getNetAmount()),
				CURRENCY,
				withdrawal.getStatus().name(),
				"LEGACY_TICKET_PAYOUT",
				null,
				withdrawal.getNotes(),
				withdrawal.getRequestedAt() == null ? null : OffsetDateTime.ofInstant(withdrawal.getRequestedAt(), ZoneOffset.UTC),
				withdrawal.getProcessedAt() == null ? null : OffsetDateTime.ofInstant(withdrawal.getProcessedAt(), ZoneOffset.UTC),
				null,
				null,
				withdrawal.getStatus().name(),
				money(withdrawal.getNetAmount()),
				CURRENCY);
	}

	private TrackerUser owner(String actorUsername) {
		TrackerUser owner = trackerUserService.getUserByUsername(required(actorUsername, "Authenticated owner is required"));
		if (owner.getPrivilege() == null || owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can view or withdraw the unified business balance");
		}
		return owner;
	}

	private Business business(TrackerUser owner) {
		if (owner.getBusiness() == null || owner.getBusiness().getId() == null) {
			throw new IllegalArgumentException("Owner is not linked to a business");
		}
		return owner.getBusiness();
	}

	private String paypalEmail(String value) {
		String email = required(value, "A PayPal email address is required").toLowerCase(Locale.ROOT);
		int at = email.indexOf('@');
		if (at <= 0 || at == email.length() - 1 || email.indexOf('.', at) < at + 2) {
			throw new IllegalArgumentException("Enter a valid PayPal email address");
		}
		return email;
	}

	private BigDecimal money(BigDecimal amount) {
		return amount == null
				? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String optional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
		return value.trim();
	}
}
