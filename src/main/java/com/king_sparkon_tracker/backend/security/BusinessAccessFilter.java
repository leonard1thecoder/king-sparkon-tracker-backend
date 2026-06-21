package com.king_sparkon_tracker.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.king_sparkon_tracker.backend.service.BusinessAccessService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BusinessAccessFilter extends OncePerRequestFilter {

	private static final List<String> EXCLUDED_PREFIXES = List.of(
			"/health",
			"/api/health",
			"/ready",
			"/api/ready",
			"/api/auth",
			"/api/billing",
			"/api/admin",
			"/api/paypal/webhooks",
			"/v3/api-docs",
			"/swagger-ui",
			"/h2-console"
	);

	private final BusinessAccessService businessAccessService;

	public BusinessAccessFilter(BusinessAccessService businessAccessService) {
		this.businessAccessService = businessAccessService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (shouldSkip(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			businessAccessService.requireActiveBusiness(authentication.getName());
			filterChain.doFilter(request, response);
		} catch (RuntimeException exception) {
			response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
			response.setContentType("application/json");
			response.getWriter().write("""
					{
					  "error": "BUSINESS_SUBSCRIPTION_INACTIVE",
					  "message": "Business subscription is inactive or deactivated. Please manage billing."
					}
					""");
		}
	}

	private boolean shouldSkip(HttpServletRequest request) {
		String path = request.getRequestURI();

		return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
	}
}
