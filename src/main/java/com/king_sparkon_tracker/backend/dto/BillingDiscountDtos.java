package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.BillingPlanDiscount;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class BillingDiscountDtos {
	private BillingDiscountDtos() {
	}

	public record UpsertBillingDiscountRequest(
			@NotNull @DecimalMin("0.00") @DecimalMax("90.00") BigDecimal discountPercent,
			@NotBlank @Size(max = 120) String label,
			boolean active,
			OffsetDateTime startsAt,
			OffsetDateTime endsAt) {
	}

	public record BillingDiscountResponse(
			Long id,
			BusinessPlan businessPlan,
			BigDecimal discountPercent,
			String label,
			boolean active,
			boolean effective,
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String updatedBy,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
		public static BillingDiscountResponse from(BillingPlanDiscount discount, OffsetDateTime now) {
			return new BillingDiscountResponse(
					discount.getId(),
					discount.getBusinessPlan(),
					discount.getDiscountPercent(),
					discount.getLabel(),
					discount.isActive(),
					discount.isEffective(now),
					discount.getStartsAt(),
					discount.getEndsAt(),
					discount.getUpdatedBy(),
					discount.getCreatedAt(),
					discount.getUpdatedAt());
		}
	}
}
