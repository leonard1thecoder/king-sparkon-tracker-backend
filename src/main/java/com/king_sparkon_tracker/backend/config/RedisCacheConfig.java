package com.king_sparkon_tracker.backend.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@Profile("redis")
public class RedisCacheConfig {

	public static final String PRIVILEGES_CACHE = "privileges";
	public static final String PRIVILEGE_BY_ROLE_CACHE = "privilegeByRole";
	public static final String PROMOTION_QUOTES_CACHE = "promotionQuotes";
	public static final String BILLING_PLANS_CACHE = "billingPlans";
	public static final String AFFILIATE_COMMISSION_TIERS_CACHE = "affiliateCommissionTiers";
	public static final String BUSINESS_PLAN_PRICES_CACHE = "businessPlanPrices";
	public static final String BUSINESS_PLAN_WORKER_LIMITS_CACHE = "businessPlanWorkerLimits";
	public static final String BUSINESS_FEATURE_ACCESS_CACHE = "businessFeatureAccess";

	@Bean
	RedisCacheConfiguration redisCacheConfiguration() {
		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(30))
				.disableCachingNullValues()
				.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
	}

	@Bean(name = "cacheManager")
	CacheManager redisCacheManager(
			RedisConnectionFactory redisConnectionFactory,
			RedisCacheConfiguration redisCacheConfiguration) {
		return RedisCacheManager.builder(redisConnectionFactory)
				.cacheDefaults(redisCacheConfiguration)
				.withInitialCacheConfigurations(cacheConfigurations(redisCacheConfiguration))
				.transactionAware()
				.build();
	}

	private Map<String, RedisCacheConfiguration> cacheConfigurations(RedisCacheConfiguration redisCacheConfiguration) {
		return Map.of(
				PRIVILEGES_CACHE, redisCacheConfiguration.entryTtl(Duration.ofHours(12)),
				PRIVILEGE_BY_ROLE_CACHE, redisCacheConfiguration.entryTtl(Duration.ofHours(12)),
				PROMOTION_QUOTES_CACHE, redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)),
				BILLING_PLANS_CACHE, redisCacheConfiguration.entryTtl(Duration.ofHours(6)),
				AFFILIATE_COMMISSION_TIERS_CACHE, redisCacheConfiguration.entryTtl(Duration.ofHours(6)),
				BUSINESS_PLAN_PRICES_CACHE, redisCacheConfiguration.entryTtl(Duration.ofHours(6)),
				BUSINESS_PLAN_WORKER_LIMITS_CACHE, redisCacheConfiguration.entryTtl(Duration.ofHours(6)),
				BUSINESS_FEATURE_ACCESS_CACHE, redisCacheConfiguration.entryTtl(Duration.ofMinutes(15))
		);
	}
}
