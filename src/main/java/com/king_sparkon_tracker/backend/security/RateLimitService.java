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
	private static final String REDIS_BACKEND = "redis";

	private final Clock clock;
	private final boolean enabled;
	private final String backend;
	private final StringRedisTemplate redisTemplate;
	private final RateLimitRule publicAuthRule;
	private final RateLimitRule freeTrialRule;
	private final RateLimitRule plusRule;
	private final RateLimitRule proRule;
	private final ConcurrentMap<String, BucketState> buckets = new ConcurrentHashMap<>();

	public RateLimitService(
			Clock clock,
			ObjectProvider<StringRedisTemplate> redisTemplateProvider,
			@Value("${app.rate-limit.enabled:true}") boolean enabled,
			@Value("${app.rate-limit.backend:memory}") String backend,
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
		this.redisTemplate = redisTemplateProvider.getIfAvailable();
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
		RateLimitRule rule = ruleFor(plan);
		return check("business:" + businessId, rule);
	}

	private RateLimitDecision check(String key, RateLimitRule rule) {
		if (!enabled || rule.limit() <= 0) {
			return new RateLimitDecision(true, rule.label(), rule.limit(), Integer.MAX_VALUE, 0, 0);
		}

		if (usesRedis()) {
			try {
				return checkWithRedis(key, rule);
			} catch (DataAccessException exception) {
				log.warn("rate_limit_redis_unavailable backend=memory rule={} reason={}", rule.label(), exception.getMessage());
			}
		}

		return checkInMemory(key, rule);
	}

	private boolean usesRedis() {
		return REDIS_BACKEND.equals(backend) && redisTemplate != null;
	}

	private RateLimitDecision checkWithRedis(String key, RateLimitRule rule) {
		long nowSeconds = clock.instant().getEpochSecond();
		long windowSeconds = Math.max(1, rule.windowSeconds());
		long retryAfterSeconds = Math.max(1, rule.retryAfterSeconds());
		long windowStart = (nowSeconds / windowSeconds) * windowSeconds;
		String baseKey = "rate-limit:" + rule.label().toLowerCase(Locale.ROOT) + ":" + key;
		String blockKey = baseKey + ":blocked";
		String counterKey = baseKey + ":" + windowStart;

		Long blockedTtl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
		if (blockedTtl != null && blockedTtl > 0) {
			return new RateLimitDecision(false, rule.label(), rule.limit(), 0, blockedTtl, blockedTtl);
		}

		Long used = redisTemplate.opsForValue().increment(counterKey);
		if (used != null && used == 1L) {
			redisTemplate.expire(counterKey, windowSeconds + 1, TimeUnit.SECONDS);
		}

		long resetAfter = secondsUntilReset(counterKey, windowStart, windowSeconds, nowSeconds);
		long usedCount = used == null ? 0 : used;

		if (usedCount > rule.limit()) {
			redisTemplate.opsForValue().set(blockKey, "1", retryAfterSeconds, TimeUnit.SECONDS);
			redisTemplate.delete(counterKey);
			return new RateLimitDecision(false, rule.label(), rule.limit(), 0, retryAfterSeconds, retryAfterSeconds);
		}

		int remaining = Math.max(0, rule.limit() - Math.toIntExact(usedCount));
		return new RateLimitDecision(true, rule.label(), rule.limit(), remaining, 0, resetAfter);
	}

	private long secondsUntilReset(String counterKey, long windowStart, long windowSeconds, long nowSeconds) {
		Long ttl = redisTemplate.getExpire(counterKey, TimeUnit.SECONDS);
		if (ttl != null && ttl > 0) {
			return ttl;
		}
		return Math.max(1, windowStart + windowSeconds - nowSeconds);
	}

	private RateLimitDecision checkInMemory(String key, RateLimitRule rule) {
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
			return new RateLimitDecision(
					true,
					rule.label(),
					rule.limit(),
					Math.max(0, rule.limit() - state.used),
					0,
					resetAfterSeconds(state, rule, now));
		}
	}

	private RateLimitDecision blockedDecision(BucketState state, RateLimitRule rule, Instant now) {
		long retryAfterSeconds = Math.max(1, Duration.between(now, state.blockedUntil).toSeconds());
		return new RateLimitDecision(
				false,
				rule.label(),
				rule.limit(),
				0,
				retryAfterSeconds,
				retryAfterSeconds);
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

			long elapsedSeconds = Duration.between(windowStart, now).toSeconds();
			if (elapsedSeconds >= rule.windowSeconds()) {
				windowStart = now;
				used = 0;
			}
		}
	}
}
