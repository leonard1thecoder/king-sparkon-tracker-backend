package com.king_sparkon_tracker.backend.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.model.BusinessAccountEntryStatus;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;
import com.king_sparkon_tracker.backend.repository.BusinessAccountLedgerEntryRepository;

@Service
@Transactional
public class FinancialLedgerService {

	private static final String CURRENCY = "ZAR";
	private static final int SCALE = 2;

	private final FinancialJournalRepository journalRepository;
	private final BusinessAccountLedgerEntryRepository legacyRepository;

	public FinancialLedgerService(
			FinancialJournalRepository journalRepository,
			BusinessAccountLedgerEntryRepository legacyRepository) {
		this.journalRepository = journalRepository;
		this.legacyRepository = legacyRepository;
	}

	public FinancialJournal postForBusinessAccountEntry(BusinessAccountLedgerEntry entry) {
		if (entry == null || entry.getId() == null || entry.getStatus() != BusinessAccountEntryStatus.POSTED) {
			throw new IllegalArgumentException("Only persisted POSTED business account entries can be journaled");
		}
		Long businessId = entry.getBusiness().getId();
		String sourceType = "BUSINESS_ACCOUNT_" + entry.getEntryType().name();
		String sourceReference = String.valueOf(entry.getId());
		return journalRepository.findByBusiness_IdAndSourceTypeAndSourceReference(businessId, sourceType, sourceReference)
				.orElseGet(() -> createJournal(entry, sourceType, sourceReference));
	}

	public FinancialReconciliationResponse reconcile(Long businessId, boolean repairMissingJournals) {
		if (repairMissingJournals) {
			legacyRepository.findByBusiness_IdOrderByCreatedDateDesc(businessId).stream()
					.filter(entry -> entry.getStatus() == BusinessAccountEntryStatus.POSTED)
					.forEach(this::postForBusinessAccountEntry);
		}
		BigDecimal legacyBalance = money(legacyRepository.sumAmountByBusinessIdAndStatus(
				businessId, BusinessAccountEntryStatus.POSTED));
		List<FinancialJournal> journals = journalRepository.findByBusiness_IdOrderByPostedAtAsc(businessId);
		BigDecimal walletBalance = BigDecimal.ZERO;
		int unbalanced = 0;
		for (FinancialJournal journal : journals) {
			BigDecimal debits = total(journal, LedgerSide.DEBIT);
			BigDecimal credits = total(journal, LedgerSide.CREDIT);
			if (debits.compareTo(credits) != 0 || !hash(journal).equals(journal.getImmutableHash())) {
				unbalanced++;
			}
			for (FinancialLedgerLine line : journal.getLines()) {
				if (line.getAccountCode() == FinancialAccountCode.BUSINESS_WALLET_LIABILITY) {
					walletBalance = walletBalance.add(
							line.getEntrySide() == LedgerSide.CREDIT ? line.getAmount() : line.getAmount().negate());
				}
			}
		}
		walletBalance = money(walletBalance);
		BigDecimal difference = money(legacyBalance.subtract(walletBalance));
		return new FinancialReconciliationResponse(
				businessId,
				legacyBalance,
				walletBalance,
				difference,
				journals.size(),
				unbalanced,
				difference.signum() == 0 && unbalanced == 0,
				Instant.now());
	}

	private FinancialJournal createJournal(
			BusinessAccountLedgerEntry entry,
			String sourceType,
			String sourceReference) {
		BigDecimal amount = money(entry.getAmount().abs());
		boolean walletCredit = entry.getAmount().signum() > 0;
		FinancialAccountCode counterparty = counterparty(entry.getEntryType(), entry.getProvider());
		String journalHash = hash(
				entry.getBusiness().getId(),
				sourceType,
				sourceReference,
				amount,
				walletCredit,
				counterparty);
		FinancialJournal journal = new FinancialJournal(
				"JRN-" + UUID.randomUUID(),
				entry.getBusiness(),
				sourceType,
				sourceReference,
				entry.getDescription(),
				CURRENCY,
				journalHash,
				entry.getCreatedBy());
		if (walletCredit) {
			journal.addLine(new FinancialLedgerLine(1, counterparty, LedgerSide.DEBIT, amount, CURRENCY, entry.getDescription()));
			journal.addLine(new FinancialLedgerLine(2, FinancialAccountCode.BUSINESS_WALLET_LIABILITY, LedgerSide.CREDIT, amount, CURRENCY, entry.getDescription()));
		} else {
			journal.addLine(new FinancialLedgerLine(1, FinancialAccountCode.BUSINESS_WALLET_LIABILITY, LedgerSide.DEBIT, amount, CURRENCY, entry.getDescription()));
			journal.addLine(new FinancialLedgerLine(2, counterparty, LedgerSide.CREDIT, amount, CURRENCY, entry.getDescription()));
		}
		return journalRepository.save(journal);
	}

	private FinancialAccountCode counterparty(BusinessAccountEntryType type, String provider) {
		return switch (type) {
			case TOP_UP -> "PAYPAL".equalsIgnoreCase(provider)
					? FinancialAccountCode.PAYPAL_CLEARING
					: FinancialAccountCode.STRIPE_CLEARING;
			case PRODUCT_SALE_CREDIT, TICKET_SALE_CREDIT, TIP_CREDIT -> FinancialAccountCode.PLATFORM_CLEARING;
			case PROMOTION_DEBIT -> FinancialAccountCode.PROMOTION_REVENUE;
			case TICKET_PROMOTION_DEBIT -> FinancialAccountCode.TICKET_PROMOTION_REVENUE;
			case OWNER_WITHDRAWAL_DEBIT -> FinancialAccountCode.OWNER_WITHDRAWAL_CLEARING;
			case OWNER_WITHDRAWAL_REVERSAL_CREDIT -> FinancialAccountCode.PAYOUT_CLEARING;
		};
	}

	private BigDecimal total(FinancialJournal journal, LedgerSide side) {
		return money(journal.getLines().stream()
				.filter(line -> line.getEntrySide() == side)
				.map(FinancialLedgerLine::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	private String hash(FinancialJournal journal) {
		FinancialLedgerLine wallet = journal.getLines().stream()
				.filter(line -> line.getAccountCode() == FinancialAccountCode.BUSINESS_WALLET_LIABILITY)
				.findFirst()
				.orElseThrow();
		FinancialLedgerLine counterparty = journal.getLines().stream()
				.filter(line -> line.getAccountCode() != FinancialAccountCode.BUSINESS_WALLET_LIABILITY)
				.findFirst()
				.orElseThrow();
		return hash(
				journal.getBusiness().getId(),
				journal.getSourceType(),
				journal.getSourceReference(),
				wallet.getAmount(),
				wallet.getEntrySide() == LedgerSide.CREDIT,
				counterparty.getAccountCode());
	}

	private String hash(
			Long businessId,
			String sourceType,
			String sourceReference,
			BigDecimal amount,
			boolean walletCredit,
			FinancialAccountCode counterparty) {
		try {
			String canonical = businessId + "|" + sourceType + "|" + sourceReference + "|" + money(amount)
					+ "|" + walletCredit + "|" + counterparty.name() + "|" + CURRENCY;
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(canonical.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Could not hash immutable journal", exception);
		}
	}

	private BigDecimal money(BigDecimal value) {
		return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, RoundingMode.HALF_UP);
	}
}
