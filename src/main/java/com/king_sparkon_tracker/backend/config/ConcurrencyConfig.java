package com.king_sparkon_tracker.backend.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configures virtual-thread execution for concurrent request and async service workloads.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ConcurrencyConfig {

	private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Africa/Johannesburg");

	/**
	 * Provides Spring's application executor with virtual threads for blocking service and database work.
	 */
	@Bean(name = "applicationTaskExecutor")
	public AsyncTaskExecutor applicationTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("king-sparkon-virtual-");
		executor.setVirtualThreads(true);
		executor.setConcurrencyLimit(500);
		return executor;
	}

	/**
	 * Centralizes application time so time-based business rules can be tested deterministically.
	 */
	@Bean
	public Clock clock() {
		return Clock.system(BUSINESS_TIME_ZONE);
	}
}
