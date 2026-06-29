package com.king_sparkon_tracker.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@Profile("!redis")
public class CacheConfig {

	@Bean
	@ConditionalOnMissingBean(CacheManager.class)
	CacheManager fallbackCacheManager() {
		return new ConcurrentMapCacheManager();
	}
}
