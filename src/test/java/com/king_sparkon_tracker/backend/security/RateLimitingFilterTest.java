package com.king_sparkon_tracker.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

class RateLimitingFilterTest {

	private final RateLimitService rateLimitService = mock(RateLimitService.class);
	private final BusinessAccessService businessAccessService = mock(BusinessAccessService.class);
	private final RateLimitingFilter filter = new RateLimitingFilter(
			rateLimitService,
			businessAccessService,
			new ObjectMapper());

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void blockedPublicAuthRequestReturnsRetryAfterContract() throws Exception {
		when(rateLimitService.checkPublicAuth("10.0.0.1:POST:/api/auth/login"))
				.thenReturn(new RateLimitDecision(false, "PUBLIC_AUTH", 2, 0, 60, 60));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
		request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("60");
		assertThat(response.getHeader("X-RateLimit-Policy")).isEqualTo("PUBLIC_AUTH");
		assertThat(response.getContentAsString()).contains("\"error\":\"RATE_LIMIT_EXCEEDED\"");
		assertThat(response.getContentAsString()).contains("\"retryAfterSeconds\":60");
	}

	@Test
	void blockedAuthenticatedFreeTrialRequestReturnsThirtySecondRetry() throws Exception {
		Business business = business(1L, BusinessPlan.FREE_TRIAL);
		when(businessAccessService.businessForActor("owner")).thenReturn(business);
		when(rateLimitService.checkBusiness("1", BusinessPlan.FREE_TRIAL))
				.thenReturn(new RateLimitDecision(false, "FREE_TRIAL", 30, 0, 30, 30));
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("owner", "password", "Owner"));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("30");
		assertThat(response.getHeader("X-RateLimit-Policy")).isEqualTo("FREE_TRIAL");
		assertThat(response.getContentAsString()).contains("\"retryAfterSeconds\":30");
		verify(rateLimitService).checkBusiness("1", BusinessPlan.FREE_TRIAL);
	}

	@Test
	void healthRequestSkipsRateLimiter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
	}

	private Business business(Long id, BusinessPlan plan) {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		business.setBusinessPlan(plan);
		ReflectionTestUtils.setField(business, "id", id);
		return business;
	}
}
