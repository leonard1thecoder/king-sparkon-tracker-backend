package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.king_sparkon_tracker.backend.model.BusinessPlan;

public record BillingPlanResponse(
		BusinessPlan plan,
		String displayName,
		BigDecimal monthlyPrice,
		String currency,
		int maxWorkers,
		boolean unlimitedWorkers,
		boolean unlimitedProducts,
		boolean unlimitedBarcodeScanning,
		boolean workerTipsPlatform,
		boolean businessAnalysisAi,
		boolean workerClocker,
		boolean affiliateProgram,
		List<AffiliateCommissionTierResponse> affiliateCommissionTiers,
		List<String> features,
		BigDecimal originalMonthlyPrice,
		BigDecimal discountPercent,
		String discountLabel,
		boolean discountActive
) {
	public BillingPlanResponse(
			BusinessPlan plan,
			String displayName,
			BigDecimal monthlyPrice,
			String currency,
			int maxWorkers,
			boolean unlimitedWorkers,
			boolean unlimitedProducts,
			boolean unlimitedBarcodeScanning,
			boolean workerTipsPlatform,
			boolean businessAnalysisAi,
			boolean workerClocker,
			boolean affiliateProgram,
			List<AffiliateCommissionTierResponse> affiliateCommissionTiers,
			List<String> features) {
		this(plan, displayName, monthlyPrice, currency, maxWorkers, unlimitedWorkers,
				unlimitedProducts, unlimitedBarcodeScanning, workerTipsPlatform,
				businessAnalysisAi, workerClocker, affiliateProgram,
				affiliateCommissionTiers, features, monthlyPrice, BigDecimal.ZERO, null, false);
	}
}
