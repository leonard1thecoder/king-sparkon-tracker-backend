package com.king_sparkon_tracker.backend.security;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitingFilter extends OncePerRequestFilter {

	private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
			"/api/auth/register",
			"/api/auth/register-affiliate",
			"/api/auth/login",
			"/api/auth/forgot-password",
			"/api/auth/reset-password",
			"/api/auth/resend-verification",
			"/api/auth/verify-email",
			"/api/contact-inquiries"
	);

	private static final Set<String> EXCLUDED_PATHS = Set.of(
			"/health",
			"/api/health",
			"/ready",
			"/api/ready",
			"/api/paypal/webhooks"
	);

	private final RateLimitService rateLimitService;
	private final BusinessAccessService businessAccessService;
	private final ObjectMapper objectMapper;

	public RateLimitingFilter(
			RateLimitService rateLimitService,
			BusinessAccessService businessAccessService,
			ObjectMapper objectMapper) {
		this.rateLimitService = rateLimitService;
		this.businessAccessService = businessAccessService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		RateLimitDecision decision = decisionFor(request);

		if (decision == null) {
			filterChain.doFilter(request, response);
			return;
		}

		writeRateLimitHeaders(response, decision);

		if (decision.allowed()) {
			filterChain.doFilter(request, response);
			return;
		}

		writeRateLimitResponse(response, decision);
	}

	private RateLimitDecision decisionFor(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (HttpMethod.OPTIONS.matches(request.getMethod()) || EXCLUDED_PATHS.contains(path) || isDocsPath(path)) {
			return null;
		}

		if (PUBLIC_AUTH_PATHS.contains(path)) {
			return rateLimitService.checkPublicAuth(clientAddress(request) + ":" + request.getMethod() + ":" + path);
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
			return null;
		}

		try {
			Business business = businessAccessService.businessForActor(authentication.getName());
			String businessId = business.getId() == null ? authentication.getName() : String.valueOf(business.getId());
			return rateLimitService.checkBusiness(businessId, business.getBusinessPlan());
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private boolean isDocsPath(String path) {
		return path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger-ui")
				|| path.startsWith("/h2-console");
	}

	private String clientAddress(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwardedFor)) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private void writeRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
		response.setHeader("X-RateLimit-Policy", decision.label());
		response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
		response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetAfterSeconds()));
		if (!decision.allowed()) {
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
		}
	}

	private void writeRateLimitResponse(HttpServletResponse response, RateLimitDecision decision) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
		body.put("error", "RATE_LIMIT_EXCEEDED");
		body.put("message", "Rate limit reached for " + decision.label()
				+ ". Please wait " + decision.retryAfterSeconds() + " seconds before retrying.");
		body.put("policy", decision.label());
		body.put("retryAfterSeconds", decision.retryAfterSeconds());
		body.put("limit", decision.limit());
		body.put("remaining", decision.remaining());

		objectMapper.writeValue(response.getWriter(), body);
	}
}
