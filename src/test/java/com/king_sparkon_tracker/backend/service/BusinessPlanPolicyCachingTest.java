package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BusinessPlanPolicyCachingTest {

	@Test
	void positiveBusinessFeaturePolicyUsesRedisCacheAnnotations() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/service/BusinessPlanPolicyService.java"));

		assertThat(source).contains("RedisCacheConfig.BUSINESS_FEATURE_ACCESS_CACHE");
		assertThat(source).contains("RedisCacheConfig.BILLING_PLANS_CACHE");
		assertThat(source).contains("RedisCacheConfig.BUSINESS_PLAN_WORKER_LIMITS_CACHE");
		assertThat(source).contains("RedisCacheConfig.BUSINESS_PLAN_PRICES_CACHE");
	}

	@Test
	void negativeFeatureAccessCacheKeyIncludesPlanStatusAndFeature() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/king_sparkon_tracker/backend/service/BusinessPlanPolicyService.java"));

		assertThat(source).contains("#business.businessPlan.name()");
		assertThat(source).contains("#business.businessStatus.name()");
		assertThat(source).contains("#feature.name()");
	}
}
