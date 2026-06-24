package com.king_sparkon_tracker.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class LocalCacheConfig {

	@Bean(name = "cacheManager")
	@ConditionalOnMissingBean(CacheManager.class)
	CacheManager localCacheManager() {
		return new ConcurrentMapCacheManager(
				RedisCacheConfig.PRIVILEGES_CACHE,
				RedisCacheConfig.PRIVILEGE_BY_ROLE_CACHE,
				RedisCacheConfig.PROMOTION_QUOTES_CACHE,
				RedisCacheConfig.BILLING_PLANS_CACHE,
				RedisCacheConfig.AFFILIATE_COMMISSION_TIERS_CACHE,
				RedisCacheConfig.BUSINESS_PLAN_PRICES_CACHE,
				RedisCacheConfig.BUSINESS_PLAN_WORKER_LIMITS_CACHE,
				RedisCacheConfig.BUSINESS_FEATURE_ACCESS_CACHE
		);
	}
}
