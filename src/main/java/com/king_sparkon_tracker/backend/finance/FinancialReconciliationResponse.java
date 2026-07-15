package com.king_sparkon_tracker.backend.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record FinancialReconciliationResponse(
		Long businessId,
		BigDecimal legacyLedgerBalance,
		BigDecimal doubleEntryWalletBalance,
		BigDecimal difference,
		int journalCount,
		int unbalancedJournalCount,
		boolean reconciled,
		Instant checkedAt) {
}
