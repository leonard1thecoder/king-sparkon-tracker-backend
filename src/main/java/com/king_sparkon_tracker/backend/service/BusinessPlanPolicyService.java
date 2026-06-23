package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.dto.AffiliateCommissionTierResponse;
import com.king_sparkon_tracker.backend.dto.BillingPlanResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;

@Service
public class BusinessPlanPolicyService {

	public static final int UNLIMITED = -1;

	private static final BigDecimal PLUS_MONTHLY_PRICE = new BigDecimal("880.00");

	private static final BigDecimal PRO_MONTHLY_PRICE = new BigDecimal("2300.00");

	private static final List<AffiliateCommissionTierResponse> AFFILIATE_COMMISSION_TIERS = List.of(
			new AffiliateCommissionTierResponse("First 3 months", 0, 3, new BigDecimal("18.00")),
			new AffiliateCommissionTierResponse("After 3 months", 3, 12, new BigDecimal("23.00")),
			new AffiliateCommissionTierResponse("After 1 year", 12, null, new BigDecimal("28.00"))
	);

	public BigDecimal monthlyPrice(BusinessPlan plan) {
		return switch (plan) {
			case FREE_TRIAL -> BigDecimal.ZERO;
			case PLUS -> PLUS_MONTHLY_PRICE;
			case PRO -> PRO_MONTHLY_PRICE;
		};
	}

	public int maxWorkers(Business business) {
		return maxWorkers(planOf(business));
	}

	public int maxWorkers(BusinessPlan plan) {
		return switch (plan) {
			case FREE_TRIAL -> 2;
			case PLUS -> 5;
			case PRO -> UNLIMITED;
		};
	}

	public boolean isActiveOrTrial(Business business) {
		if (business == null || business.getBusinessStatus() == null) {
			return false;
		}

		return business.getBusinessStatus() == BusinessStatus.TRIAL
				|| business.getBusinessStatus() == BusinessStatus.ACTIVE;
	}

	public void requireActiveOrTrial(Business business) {
		if (!isActiveOrTrial(business)) {
			throw new IllegalArgumentException("Business subscription is not active");
		}
	}

	public boolean isFeatureEnabled(Business business, BusinessFeature feature) {
		if (!isActiveOrTrial(business)) {
			return false;
		}

		BusinessPlan plan = planOf(business);

		return switch (feature) {
			case CREATE_WORKERS -> true;
			case CREATE_PRODUCTS -> true;
			case ADD_BARCODES -> true;
			case SCAN_BARCODES -> true;
			case WORKER_TIPS_PLATFORM -> plan == BusinessPlan.PRO;
			case BUSINESS_ANALYSIS_AI -> plan == BusinessPlan.PRO;
			case WORKER_CLOCKER -> plan == BusinessPlan.PRO;
		};
	}

	public void requireFeature(Business business, BusinessFeature feature) {
		requireActiveOrTrial(business);

		if (!isFeatureEnabled(business, feature)) {
			throw new IllegalArgumentException("Feature " + feature + " is not available on " + planOf(business) + " plan");
		}
	}

	public List<BillingPlanResponse> billingPlans() {
		return List.of(
				new BillingPlanResponse(
						BusinessPlan.FREE_TRIAL,
						"Free 14 day trial",
						BigDecimal.ZERO,
						"ZAR",
						2,
						false,
						true,
						true,
						false,
						false,
						false,
						true,
						AFFILIATE_COMMISSION_TIERS,
						List.of(
								"14 day trial",
								"2 workers",
								"Unlimited products",
								"Unlimited barcode scanning",
								"Affiliate promo QR tracking"
						)
				),
				new BillingPlanResponse(
						BusinessPlan.PLUS,
						"Plus",
						PLUS_MONTHLY_PRICE,
						"ZAR",
						5,
						false,
						true,
						true,
						false,
						false,
						false,
						true,
						AFFILIATE_COMMISSION_TIERS,
						List.of(
								"5 workers",
								"Unlimited products",
								"Unlimited barcode scanning",
								"Affiliate promo QR tracking"
						)
				),
				new BillingPlanResponse(
						BusinessPlan.PRO,
						"Pro",
						PRO_MONTHLY_PRICE,
						"ZAR",
						UNLIMITED,
						true,
						true,
						true,
						true,
						true,
						true,
						true,
						AFFILIATE_COMMISSION_TIERS,
						List.of(
								"Unlimited workers",
								"Unlimited products",
								"Unlimited barcode scanning",
								"Workers tips platform",
								"Business Analysis AI",
								"Worker clocker",
								"Affiliate promo QR tracking"
						)
				)
		);
	}

	public List<AffiliateCommissionTierResponse> affiliateCommissionTiers() {
		return AFFILIATE_COMMISSION_TIERS;
	}

	private BusinessPlan planOf(Business business) {
		if (business == null || business.getBusinessPlan() == null) {
			return BusinessPlan.FREE_TRIAL;
		}

		return business.getBusinessPlan();
	}
}
