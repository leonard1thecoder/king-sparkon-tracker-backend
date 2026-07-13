package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.SubscriptionPaymentStatus;

public record BillingDashboardResponse(
		Long businessId,
		String businessName,
		BusinessPlan currentPlan,
		BusinessStatus businessStatus,
		SubscriptionPaymentStatus paymentStatus,
		BillingInterval currentBillingInterval,
		Integer currentTermYears,
		boolean trial,
		long trialDaysLeft,
		LocalDateTime trialEndDate,
		LocalDateTime currentBillingPeriodEndDate,
		boolean deactivated,
		boolean showDeactivatedOverlay,
		boolean showUpgradeButtons,
		boolean canUseProducts,
		boolean canUseBarcodeScanning,
		boolean canUseWorkerTipsPlatform,
		boolean canUseBusinessAnalysisAi,
		boolean canUseWorkerClocker,
		List<BillingPlanResponse> availablePlans
) {

	public static BillingDashboardResponse from(
			Business business,
			BusinessSubscription subscription,
			long trialDaysLeft,
			boolean canUseProducts,
			boolean canUseBarcodeScanning,
			boolean canUseWorkerTipsPlatform,
			boolean canUseBusinessAnalysisAi,
			boolean canUseWorkerClocker,
			List<BillingPlanResponse> availablePlans) {
		boolean deactivated = business.getBusinessStatus() == BusinessStatus.DEACTIVATED;
		boolean trial = business.getBusinessStatus() == BusinessStatus.TRIAL;

		return new BillingDashboardResponse(
				business.getId(),
				business.getName(),
				business.getBusinessPlan(),
				business.getBusinessStatus(),
				subscription == null ? null : subscription.getStatus(),
				subscription == null ? null : subscription.getBillingInterval(),
				subscription == null ? null : subscription.getTermYears(),
				trial,
				trialDaysLeft,
				business.getTrialEndDate(),
				business.getCurrentBillingPeriodEndDate(),
				deactivated,
				deactivated,
				trial || deactivated || business.getBusinessPlan() != BusinessPlan.PRO,
				canUseProducts,
				canUseBarcodeScanning,
				canUseWorkerTipsPlatform,
				canUseBusinessAnalysisAi,
				canUseWorkerClocker,
				availablePlans
		);
	}
}
