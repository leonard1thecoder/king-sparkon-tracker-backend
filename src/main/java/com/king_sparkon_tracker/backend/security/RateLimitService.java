package com.king_sparkon_tracker.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.BusinessPlan;

@Service
public class RateLimitService {

	private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
	private static final String REDIS = "redis";

	private final Clock clock;
	private final boolean enabled;
	private final String backend;
	private final StringRedisTemplate redis;
	private final RateLimitRule publicAuthRule;
	private final RateLimitRule freeTrialRule;
	private final RateLimitRule plusRule;
	private final RateLimitRule proRule;
	private final ConcurrentMap<String, BucketState> buckets = new ConcurrentHashMap<>();

	public RateLimitService(
			Clock clock,
			ObjectProvider<StringRedisTemplate> redisProvider,
			@Value("${app.rate-limit.enabled:true}") boolean enabled,
			@Value("${RATE_LIMIT_BACKEND:${app.rate-limit.backend:memory}}") String backend,
			@Value("${app.rate-limit.public-auth.limit:10}") int publicAuthLimit,
			@Value("${app.rate-limit.public-auth.window-seconds:60}") long publicAuthWindowSeconds,
			@Value("${app.rate-limit.public-auth.retry-after-seconds:60}") long publicAuthRetryAfterSeconds,
			@Value("${app.rate-limit.free-trial.limit:30}") int freeTrialLimit,
			@Value("${app.rate-limit.free-trial.window-seconds:60}") long freeTrialWindowSeconds,
			@Value("${app.rate-limit.free-trial.retry-after-seconds:30}") long freeTrialRetryAfterSeconds,
			@Value("${app.rate-limit.plus.limit:120}") int plusLimit,
			@Value("${app.rate-limit.plus.window-seconds:60}") long plusWindowSeconds,
			@Value("${app.rate-limit.plus.retry-after-seconds:15}") long plusRetryAfterSeconds,
			@Value("${app.rate-limit.pro.limit:600}") int proLimit,
			@Value("${app.rate-limit.pro.window-seconds:60}") long proWindowSeconds,
			@Value("${app.rate-limit.pro.retry-after-seconds:5}") long proRetryAfterSeconds) {
		this.clock = clock;
		this.redis = redisProvider.getIfAvailable();
		this.enabled = enabled;
		this.backend = backend == null ? "memory" : backend.trim().toLowerCase(Locale.ROOT);
		this.publicAuthRule = new RateLimitRule("PUBLIC_AUTH", null, publicAuthLimit, publicAuthWindowSeconds, publicAuthRetryAfterSeconds);
		this.freeTrialRule = new RateLimitRule("FREE_TRIAL", BusinessPlan.FREE_TRIAL, freeTrialLimit, freeTrialWindowSeconds, freeTrialRetryAfterSeconds);
		this.plusRule = new RateLimitRule("PLUS", BusinessPlan.PLUS, plusLimit, plusWindowSeconds, plusRetryAfterSeconds);
		this.proRule = new RateLimitRule("PRO", BusinessPlan.PRO, proLimit, proWindowSeconds, proRetryAfterSeconds);
	}

	public RateLimitDecision checkPublicAuth(String key) {
		return check("public-auth:" + key, publicAuthRule);
	}

	public RateLimitDecision checkBusiness(String businessId, BusinessPlan plan) {
		return check("business:" + businessId, ruleFor(plan));
	}

	private RateLimitDecision check(String key, RateLimitRule rule) {
		if (!enabled || rule.limit() <= 0) {
			return new RateLimitDecision(true, rule.label(), rule.limit(), Integer.MAX_VALUE, 0, 0);
		}
		if (REDIS.equals(backend) && redis != null) {
			try {
				return checkRedis(key, rule);
			} catch (DataAccessException ex) {
				log.warn("rate_limit_redis_unavailable fallback=memory rule={} reason={}", rule.label(), ex.getMessage());
			}
		}
		return checkMemory(key, rule);
	}

	private RateLimitDecision checkRedis(String key, RateLimitRule rule) {
		long now = clock.instant().getEpochSecond();
		long window = Math.max(1, rule.windowSeconds());
		long windowStart = (now / window) * window;
		String base = "rate-limit:" + rule.label().toLowerCase(Locale.ROOT) + ":" + key;
		String blockKey = base + ":blocked";
		String countKey = base + ":" + windowStart;

		Long blockedTtl = redis.getExpire(blockKey, TimeUnit.SECONDS);
		if (blockedTtl != null && blockedTtl > 0) {
			return new RateLimitDecision(false, rule.label(), rule.limit(), 0, blockedTtl, blockedTtl);
		}

		Long used = redis.opsForValue().increment(countKey);
		if (used != null && used == 1L) {
			redis.expire(countKey, window + 1, TimeUnit.SECONDS);
		}

		long usedCount = used == null ? 0 : used;
		long reset = resetSeconds(countKey, windowStart, window, now);
		if (usedCount > rule.limit()) {
			long retry = Math.max(1, rule.retryAfterSeconds());
			redis.opsForValue().set(blockKey, "1", retry, TimeUnit.SECONDS);
			redis.delete(countKey);
			return new RateLimitDecision(false, rule.label(), rule.limit(), 0, retry, retry);
		}
		int remaining = Math.max(0, rule.limit() - (int) Math.min(Integer.MAX_VALUE, usedCount));
		return new RateLimitDecision(true, rule.label(), rule.limit(), remaining, 0, reset);
	}

	private long resetSeconds(String countKey, long windowStart, long window, long now) {
		Long ttl = redis.getExpire(countKey, TimeUnit.SECONDS);
		return ttl != null && ttl > 0 ? ttl : Math.max(1, windowStart + window - now);
	}

	private RateLimitDecision checkMemory(String key, RateLimitRule rule) {
		Instant now = clock.instant();
		BucketState state = buckets.computeIfAbsent(key, ignored -> new BucketState(now));
		synchronized (state) {
			state.refresh(now, rule);
			if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
				return blockedDecision(state, rule, now);
			}
			if (state.used >= rule.limit()) {
				state.blockedUntil = now.plusSeconds(rule.retryAfterSeconds());
				state.used = 0;
				state.windowStart = state.blockedUntil;
				return blockedDecision(state, rule, now);
			}
			state.used++;
			return new RateLimitDecision(true, rule.label(), rule.limit(), Math.max(0, rule.limit() - state.used), 0, resetAfterSeconds(state, rule, now));
		}
	}

	private RateLimitDecision blockedDecision(BucketState state, RateLimitRule rule, Instant now) {
		long retry = Math.max(1, Duration.between(now, state.blockedUntil).toSeconds());
		return new RateLimitDecision(false, rule.label(), rule.limit(), 0, retry, retry);
	}

	private long resetAfterSeconds(BucketState state, RateLimitRule rule, Instant now) {
		return Math.max(1, rule.windowSeconds() - Duration.between(state.windowStart, now).toSeconds());
	}

	private RateLimitRule ruleFor(BusinessPlan plan) {
		if (plan == BusinessPlan.PLUS) {
			return plusRule;
		}
		if (plan == BusinessPlan.PRO) {
			return proRule;
		}
		return freeTrialRule;
	}

	private static class BucketState {
		private Instant windowStart;
		private Instant blockedUntil;
		private int used;

		private BucketState(Instant windowStart) {
			this.windowStart = windowStart;
		}

		private void refresh(Instant now, RateLimitRule rule) {
			if (blockedUntil != null && !blockedUntil.isAfter(now)) {
				blockedUntil = null;
				windowStart = now;
				used = 0;
				return;
			}
			if (Duration.between(windowStart, now).toSeconds() >= rule.windowSeconds()) {
				windowStart = now;
				used = 0;
			}
		}
	}
}
