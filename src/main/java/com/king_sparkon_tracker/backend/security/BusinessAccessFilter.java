package com.king_sparkon_tracker.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BusinessAccessFilter extends OncePerRequestFilter {

	private static final Set<String> BUSINESS_SCOPED_AUTHORITIES = Set.of(
			PrivilegeRole.Owner.name(),
			PrivilegeRole.Worker.name(),
			PrivilegeRole.Affiliate.name()
	);

	private static final List<String> EXCLUDED_PREFIXES = List.of(
			"/health",
			"/api/health",
			"/ready",
			"/api/ready",
			"/api/auth",
			"/api/contact-inquiries",
			"/api/subscribers",
			"/api/affiliate-links/public",
			"/api/affiliate-links/random",
			"/api/affiliates",
			"/api/billing",
			"/api/users/me",
			"/api/admin",
			"/api/paypal/webhooks",
			"/api/stripe/webhooks",
			"/api/v1/tickets/events",
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

		if (!requiresActiveBusiness(authentication)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			businessAccessService.requireActiveBusiness(authentication.getName());
			filterChain.doFilter(request, response);
		} catch (RuntimeException exception) {
			response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
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

	private boolean requiresActiveBusiness(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(BUSINESS_SCOPED_AUTHORITIES::contains);
	}
}