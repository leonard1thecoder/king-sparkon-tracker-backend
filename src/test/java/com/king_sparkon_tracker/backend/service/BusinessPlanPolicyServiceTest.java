package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	void billingPlansExposeFreePlusAndProTiers() {
		assertThat(service.billingPlans())
				.extracting("plan")
				.containsExactly(BusinessPlan.FREE_TRIAL, BusinessPlan.PLUS, BusinessPlan.PRO);

		assertThat(service.monthlyPrice(BusinessPlan.FREE_TRIAL)).isZero();
		assertThat(service.monthlyPrice(BusinessPlan.PLUS)).isEqualByComparingTo("880.00");
		assertThat(service.monthlyPrice(BusinessPlan.PRO)).isEqualByComparingTo("2300.00");
		assertThat(service.maxWorkers(BusinessPlan.FREE_TRIAL)).isEqualTo(2);
		assertThat(service.maxWorkers(BusinessPlan.PLUS)).isEqualTo(5);
		assertThat(service.maxWorkers(BusinessPlan.PRO)).isEqualTo(BusinessPlanPolicyService.UNLIMITED);
	}

	@Test
	void enabledFeaturesRespectPlanAndStatus() {
		Business trial = business(BusinessPlan.FREE_TRIAL, BusinessStatus.TRIAL);
		Business plus = business(BusinessPlan.PLUS, BusinessStatus.ACTIVE);
		Business pro = business(BusinessPlan.PRO, BusinessStatus.ACTIVE);
		Business deactivated = business(BusinessPlan.PRO, BusinessStatus.DEACTIVATED);

		assertThat(service.isFeatureEnabled(trial, BusinessFeature.CREATE_PRODUCTS)).isTrue();
		assertThat(service.isFeatureEnabled(plus, BusinessFeature.SCAN_BARCODES)).isTrue();
		assertThat(service.isFeatureEnabled(plus, BusinessFeature.BUSINESS_ANALYSIS_AI)).isFalse();
		assertThat(service.isFeatureEnabled(pro, BusinessFeature.BUSINESS_ANALYSIS_AI)).isTrue();
		assertThat(service.isFeatureEnabled(deactivated, BusinessFeature.CREATE_PRODUCTS)).isFalse();
	}

	@Test
	void requireActiveOrTrialRejectsInactiveBusinesses() {
		assertThatThrownBy(() -> service.requireActiveOrTrial(business(BusinessPlan.PLUS, BusinessStatus.PAST_DUE)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Business subscription is not active");
	}

	@Test
	void requireFeatureRejectsProOnlyFeatureOnPlusPlan() {
		assertThatThrownBy(() -> service.requireFeature(
				business(BusinessPlan.PLUS, BusinessStatus.ACTIVE),
				BusinessFeature.WORKER_CLOCKER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Feature WORKER_CLOCKER is not available on PLUS plan");
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
