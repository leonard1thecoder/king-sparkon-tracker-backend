package com.king_sparkon_tracker.backend.logging;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Adds a request id to every request and writes one production-safe completion log line.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final String REQUEST_ID_MDC_KEY = "requestId";

	/**
	 * Wraps the filter chain so each request has traceable timing, status, actor, and path metadata.
	 */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = requestIdFrom(request);
		long startedAt = System.nanoTime();
		String exceptionName = null;

		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		try {
			filterChain.doFilter(request, response);
		}
		catch (IOException | ServletException | RuntimeException ex) {
			exceptionName = ex.getClass().getSimpleName();
			throw ex;
		}
		finally {
			long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
			logRequestCompletion(request, response, durationMs, exceptionName);
			MDC.remove(REQUEST_ID_MDC_KEY);
		}
	}

	/**
	 * Reuses an inbound request id when trusted infrastructure supplies one, otherwise generates a UUID.
	 */
	private String requestIdFrom(HttpServletRequest request) {
		String inboundRequestId = request.getHeader(REQUEST_ID_HEADER);
		if (inboundRequestId == null || inboundRequestId.isBlank()) {
			return UUID.randomUUID().toString();
		}
		return inboundRequestId.trim();
	}

	/**
	 * Logs request metadata without request bodies, credentials, tokens, or raw query strings.
	 */
	private void logRequestCompletion(
			HttpServletRequest request,
			HttpServletResponse response,
			long durationMs,
			String exceptionName) {
		String username = usernameFrom(request);
		int status = response.getStatus();
		if (exceptionName == null) {
			log.info(
					"http_request_completed method={} path={} status={} durationMs={} actor={}",
					request.getMethod(),
					request.getRequestURI(),
					status,
					durationMs,
					username);
			return;
		}

		log.warn(
				"http_request_failed method={} path={} status={} durationMs={} actor={} exception={}",
				request.getMethod(),
				request.getRequestURI(),
				status,
				durationMs,
				username,
				exceptionName);
	}

	/**
	 * Reads the authenticated principal name when security has resolved one; otherwise marks the caller anonymous.
	 */
	private String usernameFrom(HttpServletRequest request) {
		Principal principal = request.getUserPrincipal();
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "anonymous";
		}
		return principal.getName();
	}
}
