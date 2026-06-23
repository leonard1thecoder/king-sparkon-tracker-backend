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
		List<String> features
) {
}
