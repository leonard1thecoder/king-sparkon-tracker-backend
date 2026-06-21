package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

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
