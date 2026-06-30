package com.king_sparkon_tracker.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncEmailConfig {

	private static final Logger log = LoggerFactory.getLogger(AsyncEmailConfig.class);
	private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);

	@Bean(name = "emailTaskExecutor", destroyMethod = "shutdownNow")
	public ThreadPoolExecutor emailTaskExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1,
				3,
				30L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(50),
				emailThreadFactory());
		executor.allowCoreThreadTimeOut(true);
		executor.setRejectedExecutionHandler((task, rejectedExecutor) -> {
			if (rejectedExecutor.isShutdown()) {
				log.warn("email_async_task_rejected reason=executor_shutdown active={} queued={}",
						rejectedExecutor.getActiveCount(), rejectedExecutor.getQueue().size());
				return;
			}
			new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(task, rejectedExecutor);
		});
		return executor;
	}

	private ThreadFactory emailThreadFactory() {
		return task -> {
			Thread thread = new Thread(task, "email-async-" + THREAD_NUMBER.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
	}

	@Bean(name = "emailAsyncExecutor")
	public Executor emailAsyncExecutor(ThreadPoolExecutor emailTaskExecutor) {
		return emailTaskExecutor;
	}
}
