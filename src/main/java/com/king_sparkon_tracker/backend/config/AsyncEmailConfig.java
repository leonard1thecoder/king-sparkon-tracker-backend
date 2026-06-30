package com.king_sparkon_tracker.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncEmailConfig {

	private static final Logger log = LoggerFactory.getLogger(AsyncEmailConfig.class);

	@Bean(name = "emailTaskExecutor")
	public Executor emailTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(3);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("email-async-");
		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.setAwaitTerminationSeconds(5);
		executor.setRejectedExecutionHandler(emailRejectedExecutionHandler());
		executor.initialize();
		return executor;
	}

	private RejectedExecutionHandler emailRejectedExecutionHandler() {
		return (task, executor) -> {
			if (executor.isShutdown()) {
				log.warn("email_async_task_rejected reason=executor_shutdown active={} queued={}",
						executor.getActiveCount(), executor.getQueue().size());
				return;
			}
			new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(task, executor);
		};
	}
}
