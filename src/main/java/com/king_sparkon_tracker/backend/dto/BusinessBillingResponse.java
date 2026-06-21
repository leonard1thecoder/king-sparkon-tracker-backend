package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.SubscriptionPaymentStatus;

public record BusinessBillingResponse(
		Long businessId,
		String businessName,
		BusinessPlan businessPlan,
		BusinessStatus businessStatus,
		LocalDateTime trialStartDate,
		LocalDateTime trialEndDate,
		LocalDateTime currentBillingPeriodStartDate,
		LocalDateTime currentBillingPeriodEndDate,
		String paypalSubscriptionId,
		Long subscriptionId,
		BillingInterval billingInterval,
		Integer termYears,
		BigDecimal amount,
		String currency,
		SubscriptionPaymentStatus paymentStatus,
		String paypalApprovalUrl
) {

	public static BusinessBillingResponse from(Business business, BusinessSubscription subscription) {
		return new BusinessBillingResponse(
				business.getId(),
				business.getName(),
				business.getBusinessPlan(),
				business.getBusinessStatus(),
				business.getTrialStartDate(),
				business.getTrialEndDate(),
				business.getCurrentBillingPeriodStartDate(),
				business.getCurrentBillingPeriodEndDate(),
				business.getPaypalSubscriptionId(),
				subscription == null ? null : subscription.getId(),
				subscription == null ? null : subscription.getBillingInterval(),
				subscription == null ? null : subscription.getTermYears(),
				subscription == null ? null : subscription.getAmount(),
				subscription == null ? null : subscription.getCurrency(),
				subscription == null ? null : subscription.getStatus(),
				subscription == null ? null : subscription.getPaypalApprovalUrl()
		);
	}
}
