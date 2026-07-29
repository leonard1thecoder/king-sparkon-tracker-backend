package com.king_sparkon_tracker.backend.security;

import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.king_sparkon_tracker.backend.service.BusinessAccessService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BusinessAccessFilter extends OncePerRequestFilter {

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
