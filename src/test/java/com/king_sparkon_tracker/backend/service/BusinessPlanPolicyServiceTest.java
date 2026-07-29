package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

class BusinessPlanPolicyServiceTest {

	private final BusinessPlanPolicyService service = new BusinessPlanPolicyService();

	@Test
	void billingPlansExposeOnlyFreeAccessTier() {
		assertThat(service.billingPlans())
				.extracting("plan")
				.containsExactly(BusinessPlan.PRO);

		assertThat(service.monthlyPrice(BusinessPlan.FREE_TRIAL)).isZero();
		assertThat(service.monthlyPrice(BusinessPlan.PLUS)).isZero();
		assertThat(service.monthlyPrice(BusinessPlan.PRO)).isZero();
		assertThat(service.maxWorkers(BusinessPlan.FREE_TRIAL)).isEqualTo(BusinessPlanPolicyService.UNLIMITED);
		assertThat(service.maxWorkers(BusinessPlan.PLUS)).isEqualTo(BusinessPlanPolicyService.UNLIMITED);
		assertThat(service.maxWorkers(BusinessPlan.PRO)).isEqualTo(BusinessPlanPolicyService.UNLIMITED);
		assertThat(service.billingPlans())
				.allSatisfy(plan -> {
					assertThat(plan.displayName()).isEqualTo("Free access");
					assertThat(plan.monthlyPrice()).isZero();
					assertThat(plan.affiliateProgram()).isTrue();
					assertThat(plan.affiliateCommissionTiers())
							.extracting("commissionRatePercent")
							.containsExactly(
									new java.math.BigDecimal("18.00"),
									new java.math.BigDecimal("23.00"),
									new java.math.BigDecimal("28.00"));
				});
	}

	@Test
	void enabledFeaturesAreAvailableForAnyLinkedBusiness() {
		Business trial = business(BusinessPlan.FREE_TRIAL, BusinessStatus.TRIAL);
		Business plus = business(BusinessPlan.PLUS, BusinessStatus.ACTIVE);
		Business pro = business(BusinessPlan.PRO, BusinessStatus.ACTIVE);
		Business deactivated = business(BusinessPlan.PRO, BusinessStatus.DEACTIVATED);

		assertThat(service.isFeatureEnabled(trial, BusinessFeature.CREATE_PRODUCTS)).isTrue();
		assertThat(service.isFeatureEnabled(plus, BusinessFeature.SCAN_BARCODES)).isTrue();
		assertThat(service.isFeatureEnabled(plus, BusinessFeature.BUSINESS_ANALYSIS_AI)).isTrue();
		assertThat(service.isFeatureEnabled(pro, BusinessFeature.BUSINESS_ANALYSIS_AI)).isTrue();
		assertThat(service.isFeatureEnabled(deactivated, BusinessFeature.CREATE_PRODUCTS)).isTrue();
		assertThat(service.isFeatureEnabled(null, BusinessFeature.CREATE_PRODUCTS)).isFalse();
	}

	@Test
	void requireActiveOrTrialAllowsAnyLinkedBusinessStatus() {
		service.requireActiveOrTrial(business(BusinessPlan.PLUS, BusinessStatus.PAST_DUE));
		service.requireActiveOrTrial(business(BusinessPlan.PRO, BusinessStatus.DEACTIVATED));
	}

	@Test
	void requireFeatureAllowsAllFeaturesOnAnyLinkedBusiness() {
		service.requireFeature(
				business(BusinessPlan.PLUS, BusinessStatus.ACTIVE),
				BusinessFeature.WORKER_CLOCKER);
	}

	private Business business(BusinessPlan plan, BusinessStatus status) {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		business.setBusinessPlan(plan);
		business.setBusinessStatus(status);
		owner.setBusiness(business);
		return business;
	}
}
