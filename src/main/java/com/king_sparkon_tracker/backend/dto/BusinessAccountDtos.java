package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.BusinessAccountEntryStatus;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class BusinessAccountDtos {
	private BusinessAccountDtos() {
	}

	public record BusinessAccountTopUpRequest(
			@NotNull @DecimalMin("1.00") BigDecimal amount,
			@Size(max = 2048) String callbackUrl,
			@Size(max = 40) String paymentMethod
	) {
	}

	public record BusinessAccountSummaryResponse(
			Long businessId,
			String businessName,
			BigDecimal availableBalance,
			List<BusinessAccountLedgerEntryResponse> recentEntries
	) {
	}

	public record BusinessAccountLedgerEntryResponse(
			Long id,
			Long businessId,
			BusinessAccountEntryType entryType,
			BusinessAccountEntryStatus status,
			BigDecimal amount,
			BigDecimal balanceAfter,
			String provider,
			String providerReference,
			String checkoutUrl,
			String description,
			String createdBy,
			OffsetDateTime createdDate,
			OffsetDateTime modifiedDate
	) {
		public static BusinessAccountLedgerEntryResponse from(BusinessAccountLedgerEntry entry) {
			return new BusinessAccountLedgerEntryResponse(
					entry.getId(),
					entry.getBusiness().getId(),
					entry.getEntryType(),
					entry.getStatus(),
					entry.getAmount(),
					entry.getBalanceAfter(),
					entry.getProvider(),
					entry.getProviderReference(),
					entry.getCheckoutUrl(),
					entry.getDescription(),
					entry.getCreatedBy(),
					entry.getCreatedDate(),
					entry.getModifiedDate());
		}
	}
}
