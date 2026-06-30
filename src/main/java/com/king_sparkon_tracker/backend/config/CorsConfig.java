package com.king_sparkon_tracker.backend.config;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:}") String allowedOrigins,
			@Value("${app.cors.allowed-origin-patterns:}") String allowedOriginPatterns,
			@Value("${app.frontend.reset-password-url:}") String frontendResetPasswordUrl,
			@Value("${app.frontend.email-verification-url:}") String frontendEmailVerificationUrl,
			@Value("${app.frontend.login-url:}") String frontendLoginUrl,
			@Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") String allowedMethods,
			@Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,Origin,X-Requested-With}") String allowedHeaders,
			@Value("${app.cors.exposed-headers:Authorization}") String exposedHeaders,
			@Value("${app.cors.allow-credentials:true}") boolean allowCredentials,
			@Value("${app.cors.max-age:3600}") long maxAge) {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = allowedOrigins(allowedOrigins,
				frontendResetPasswordUrl,
				frontendEmailVerificationUrl,
				frontendLoginUrl);
		List<String> originPatterns = csvValues(allowedOriginPatterns);

		if (!origins.isEmpty()) {
			configuration.setAllowedOrigins(origins);
		}
		if (!originPatterns.isEmpty()) {
			configuration.setAllowedOriginPatterns(originPatterns);
		}

		configuration.setAllowedMethods(csvValues(allowedMethods));
		configuration.setAllowedHeaders(csvValues(allowedHeaders));
		configuration.setExposedHeaders(csvValues(exposedHeaders));
		configuration.setAllowCredentials(allowCredentials);
		configuration.setMaxAge(maxAge);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private List<String> allowedOrigins(String allowedOrigins, String... frontendUrls) {
		LinkedHashSet<String> values = new LinkedHashSet<>(csvValues(allowedOrigins));
		Arrays.stream(frontendUrls)
				.map(this::originFromUrl)
				.filter(StringUtils::hasText)
				.forEach(values::add);
		return List.copyOf(values);
	}

	private String originFromUrl(String url) {
		if (!StringUtils.hasText(url)) {
			return "";
		}

		try {
			URI uri = URI.create(url.trim());
			if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
				return "";
			}
			String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
			return uri.getScheme() + "://" + uri.getHost() + port;
		} catch (IllegalArgumentException ignored) {
			return "";
		}
	}

	private List<String> csvValues(String value) {
		if (!StringUtils.hasText(value)) {
			return List.of();
		}
		return Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.toList();
	}
}
