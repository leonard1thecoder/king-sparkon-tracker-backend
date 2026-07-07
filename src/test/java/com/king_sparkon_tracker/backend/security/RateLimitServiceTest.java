package com.king_sparkon_tracker.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.king_sparkon_tracker.backend.model.BusinessPlan;

class RateLimitServiceTest {

	private final MutableClock clock = new MutableClock();

	@Test
	void freeTrialLimitUsesThirtySecondRetry() {
		RateLimitService service = serviceWithPlanLimits(2, 4, 6);

		assertThat(service.checkBusiness("1", BusinessPlan.FREE_TRIAL).allowed()).isTrue();
		assertThat(service.checkBusiness("1", BusinessPlan.FREE_TRIAL).allowed()).isTrue();

		RateLimitDecision blocked = service.checkBusiness("1", BusinessPlan.FREE_TRIAL);

		assertThat(blocked.allowed()).isFalse();
		assertThat(blocked.label()).isEqualTo("FREE_TRIAL");
		assertThat(blocked.retryAfterSeconds()).isEqualTo(30);

		clock.advance(Duration.ofSeconds(30));
		assertThat(service.checkBusiness("1", BusinessPlan.FREE_TRIAL).allowed()).isTrue();
	}

	@Test
	void plusLimitUsesHigherCapacityAndFifteenSecondRetry() {
		RateLimitService service = serviceWithPlanLimits(2, 4, 6);

		for (int index = 0; index < 4; index++) {
			assertThat(service.checkBusiness("1", BusinessPlan.PLUS).allowed()).isTrue();
		}

		RateLimitDecision blocked = service.checkBusiness("1", BusinessPlan.PLUS);

		assertThat(blocked.allowed()).isFalse();
		assertThat(blocked.label()).isEqualTo("PLUS");
		assertThat(blocked.retryAfterSeconds()).isEqualTo(15);
	}

	@Test
	void proLimitUsesLargestCapacity() {
		RateLimitService service = serviceWithPlanLimits(2, 4, 6);

		for (int index = 0; index < 6; index++) {
			assertThat(service.checkBusiness("1", BusinessPlan.PRO).allowed()).isTrue();
		}

		RateLimitDecision blocked = service.checkBusiness("1", BusinessPlan.PRO);

		assertThat(blocked.allowed()).isFalse();
		assertThat(blocked.label()).isEqualTo("PRO");
		assertThat(blocked.retryAfterSeconds()).isEqualTo(5);
	}

	@Test
	void publicAuthLimitIsKeyedSeparatelyFromBusinessLimit() {
		RateLimitService service = serviceWithPlanLimits(2, 4, 6);

		assertThat(service.checkPublicAuth("10.0.0.1:POST:/api/auth/login").allowed()).isTrue();
		assertThat(service.checkPublicAuth("10.0.0.1:POST:/api/auth/login").allowed()).isTrue();

		RateLimitDecision blocked = service.checkPublicAuth("10.0.0.1:POST:/api/auth/login");

		assertThat(blocked.allowed()).isFalse();
		assertThat(blocked.label()).isEqualTo("PUBLIC_AUTH");
		assertThat(blocked.retryAfterSeconds()).isEqualTo(60);
		assertThat(service.checkPublicAuth("10.0.0.2:POST:/api/auth/login").allowed()).isTrue();
	}

	private RateLimitService serviceWithPlanLimits(int freeTrialLimit, int plusLimit, int proLimit) {
		@SuppressWarnings("unchecked")
		ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
		when(redisProvider.getIfAvailable()).thenReturn(null);

		return new RateLimitService(
				clock,
				redisProvider,
				true,
				"memory",
				2,
				60,
				60,
				20,
				60,
				60,
				freeTrialLimit,
				60,
				30,
				plusLimit,
				60,
				15,
				proLimit,
				60,
				5);
	}

	private static class MutableClock extends Clock {

		private Instant instant = Instant.parse("2026-06-12T00:00:00Z");

		@Override
		public ZoneId getZone() {
			return ZoneId.of("Africa/Johannesburg");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}
	}
}
