package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.config.RedisCacheConfig;
import com.king_sparkon_tracker.backend.dto.AffiliateCommissionTierResponse;
import com.king_sparkon_tracker.backend.dto.BillingPlanResponse;
import com.king_sparkon_tracker.backend.model.BillingPlanDiscount;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;

@Service
public class BusinessPlanPolicyService {

	public static final int UNLIMITED = -1;
	private static final List<AffiliateCommissionTierResponse> AFFILIATE_COMMISSION_TIERS = List.of(
			new AffiliateCommissionTierResponse("First 3 months", 0, 3, new BigDecimal("18.00")),
			new AffiliateCommissionTierResponse("After 3 months", 3, 12, new BigDecimal("23.00")),
			new AffiliateCommissionTierResponse("After 1 year", 12, null, new BigDecimal("28.00"))
	);

	private BillingPlanDiscountService discountService;

	@Autowired(required = false)
	void setDiscountService(BillingPlanDiscountService discountService) {
		this.discountService = discountService;
	}

	@Cacheable(cacheNames = RedisCacheConfig.BUSINESS_PLAN_PRICES_CACHE, key = "#plan.name()")
	public BigDecimal monthlyPrice(BusinessPlan plan) {
		BigDecimal original = originalMonthlyPrice(plan);
		return discountService == null ? original : discountService.effectivePrice(plan, original);
	}

	public BigDecimal originalMonthlyPrice(BusinessPlan plan) {
		return BigDecimal.ZERO;
	}

	public int maxWorkers(Business business) {
		return maxWorkers(planOf(business));
	}

	@Cacheable(cacheNames = RedisCacheConfig.BUSINESS_PLAN_WORKER_LIMITS_CACHE, key = "#plan.name()")
	public int maxWorkers(BusinessPlan plan) {
		return UNLIMITED;
	}

	public boolean isActiveOrTrial(Business business) {
		return business != null;
	}

	public void requireActiveOrTrial(Business business) {
		if (!isActiveOrTrial(business)) throw new IllegalArgumentException("Business subscription is not active");
	}

	@Cacheable(
			cacheNames = RedisCacheConfig.BUSINESS_FEATURE_ACCESS_CACHE,
			key = "(#business == null ? 'NO_BUSINESS' : #business.id) + ':' + (#business == null || #business.businessPlan == null ? 'FREE_TRIAL' : #business.businessPlan.name()) + ':' + (#business == null || #business.businessStatus == null ? 'NO_STATUS' : #business.businessStatus.name()) + ':' + #feature.name()")
	public boolean isFeatureEnabled(Business business, BusinessFeature feature) {
		return business != null;
	}

	public void requireFeature(Business business, BusinessFeature feature) {
		requireActiveOrTrial(business);
		if (!isFeatureEnabled(business, feature)) {
			throw new IllegalArgumentException("Feature " + feature + " is not available on " + planOf(business) + " plan");
		}
	}

	@Cacheable(cacheNames = RedisCacheConfig.BILLING_PLANS_CACHE)
	public List<BillingPlanResponse> billingPlans() {
		return List.of(planResponse(BusinessPlan.PRO, "Free access", UNLIMITED, true, true, true, true, true, true,
				List.of("Unlimited workers", "Unlimited products", "Unlimited barcode scanning", "Worker tips platform", "Business Analysis AI", "Worker clocker", "Affiliate promo QR tracking")));
	}

	private BillingPlanResponse planResponse(
			BusinessPlan plan,
			String displayName,
			int maxWorkers,
			boolean unlimitedWorkers,
			boolean unlimitedProducts,
			boolean unlimitedBarcodeScanning,
			boolean workerTipsPlatform,
			boolean businessAnalysisAi,
			boolean workerClocker,
			List<String> features) {
		BigDecimal originalPrice = originalMonthlyPrice(plan);
		BigDecimal effectivePrice = monthlyPrice(plan);
		BillingPlanDiscount discount = discountService == null ? null : discountService.effectiveDiscount(plan).orElse(null);
		return new BillingPlanResponse(
				plan, displayName, effectivePrice, "ZAR", maxWorkers, unlimitedWorkers, unlimitedProducts,
				unlimitedBarcodeScanning, workerTipsPlatform, businessAnalysisAi, workerClocker, true,
				AFFILIATE_COMMISSION_TIERS, features, originalPrice,
				discount == null ? BigDecimal.ZERO : discount.getDiscountPercent(),
				discount == null ? null : discount.getLabel(), discount != null);
	}

	@Cacheable(cacheNames = RedisCacheConfig.AFFILIATE_COMMISSION_TIERS_CACHE)
	public List<AffiliateCommissionTierResponse> affiliateCommissionTiers() {
		return AFFILIATE_COMMISSION_TIERS;
	}

	private BusinessPlan planOf(Business business) {
		return business == null || business.getBusinessPlan() == null ? BusinessPlan.PRO : business.getBusinessPlan();
	}
}
