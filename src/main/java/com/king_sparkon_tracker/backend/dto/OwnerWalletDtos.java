package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.dto.BusinessAccountDtos.BusinessAccountLedgerEntryResponse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OwnerWalletDtos {

	private OwnerWalletDtos() {
	}

	public record OwnerWalletSummaryResponse(
			Long businessId,
			String businessName,
			BigDecimal availableBalance,
			BigDecimal minimumWithdrawalAmount,
			BigDecimal onlineProductRevenue,
			BigDecimal ticketRevenue,
			BigDecimal tipRevenue,
			BigDecimal topUps,
			BigDecimal promotionSpend,
			BigDecimal withdrawn,
			long pendingWithdrawalCount,
			List<BusinessAccountLedgerEntryResponse> recentEntries) {
	}

	public record OwnerWithdrawalRequest(
			@NotNull @DecimalMin("100.00") BigDecimal amount,
			@NotBlank @Size(max = 40) String payoutMethod,
			@Size(max = 320) String payoutDestination,
			@Size(max = 700) String notes) {
	}

	public record OwnerWithdrawalResponse(
			String id,
			String source,
			Long businessId,
			BigDecimal grossAmount,
			BigDecimal feeAmount,
			BigDecimal netAmount,
			String currency,
			String status,
			String payoutMethod,
			String payoutDestination,
			String notes,
			OffsetDateTime requestedAt,
			OffsetDateTime processedAt) {
	}
}
