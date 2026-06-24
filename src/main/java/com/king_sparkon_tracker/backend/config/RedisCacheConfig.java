package com.king_sparkon_tracker.backend.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig {

	public static final String PRIVILEGES_CACHE = "privileges";
	public static final String PRIVILEGE_BY_ROLE_CACHE = "privilegeByRole";
	public static final String PROMOTION_QUOTES_CACHE = "promotionQuotes";

	@Bean
	RedisCacheConfiguration redisCacheConfiguration() {
		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(30))
				.disableCachingNullValues()
				.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
	}

	@Bean
	RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
		Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
				PRIVILEGES_CACHE, redisCacheConfiguration().entryTtl(Duration.ofHours(12)),
				PRIVILEGE_BY_ROLE_CACHE, redisCacheConfiguration().entryTtl(Duration.ofHours(12)),
				PROMOTION_QUOTES_CACHE, redisCacheConfiguration().entryTtl(Duration.ofMinutes(10)));

		return builder -> builder.withInitialCacheConfigurations(cacheConfigurations);
	}

	@Bean
	CacheManagerCustomizer cacheManagerCustomizer() {
		return new CacheManagerCustomizer();
	}

	static class CacheManagerCustomizer {
		void validate(CacheManager cacheManager) {
			// Marker bean used by tests and documentation to confirm cache configuration loads.
		}
	}
}
