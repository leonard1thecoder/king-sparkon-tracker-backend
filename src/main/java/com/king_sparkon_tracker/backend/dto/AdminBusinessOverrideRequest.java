package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.AdminBusinessOverrideAction;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

import jakarta.validation.constraints.NotNull;

public record AdminBusinessOverrideRequest(
		@NotNull
		AdminBusinessOverrideAction action,

		BusinessPlan businessPlan,

		LocalDateTime currentBillingPeriodEndDate,

		String reason
) {
}
