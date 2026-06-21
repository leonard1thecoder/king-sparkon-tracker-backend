package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBusinessSubscriptionRequest(
		@Schema(description = "Paid business plan.", example = "PLUS")
		@NotNull
		BusinessPlan plan,

		@Schema(description = "Billing interval.", example = "MONTHLY")
		@NotNull
		BillingInterval billingInterval,

		@Schema(description = "Yearly term. Required for yearly plans. Must be 1 to 5.", example = "3")
		@Min(1)
		@Max(5)
		Integer termYears
) {
}
