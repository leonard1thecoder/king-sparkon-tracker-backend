package com.king_sparkon_tracker.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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
			"/api/stripe/webhooks"
	);

	public BusinessAccessFilter(BusinessAccessService businessAccessService) {
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		filterChain.doFilter(request, response);
	}
}
