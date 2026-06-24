package com.king_sparkon_tracker.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] OPENAPI_PATHS = {
			"/v3/api-docs/**",
			"/v3/api-docs.yaml",
			"/swagger-ui/**",
			"/swagger-ui.html"
	};

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter,
			BusinessAccessService businessAccessService,
			RateLimitService rateLimitService,
			ObjectMapper objectMapper,
			@Value("${app.security.h2-console-enabled:false}") boolean h2ConsoleEnabled) throws Exception {
		String ownerAuthority = PrivilegeRole.Owner.name();
		String affiliateAuthority = PrivilegeRole.Affiliate.name();

		return http
				.cors(Customizer.withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/health", "/api/health", "/ready", "/api/ready").permitAll()
						.requestMatchers(OPENAPI_PATHS).permitAll()
						.requestMatchers(
								HttpMethod.POST,
								"/api/auth/register",
								"/api/auth/register-affiliate",
								"/api/auth/login",
								"/api/auth/forgot-password",
								"/api/auth/reset-password"
						).permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/resend-verification").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/verify-email").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/contact-inquiries").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/paypal/webhooks").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/stripe/webhooks").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/affiliate-links/public/random").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/affiliate-links/*/click").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/affiliate-links/random").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/affiliate-links").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.PATCH, "/api/affiliate-links/**").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.GET, "/api/affiliate-links", "/api/affiliate-links/**").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.POST, "/api/tips").hasAuthority(PrivilegeRole.Worker.name())
						.requestMatchers("/h2-console/**").access((authentication, context) -> h2ConsoleEnabled
								? new org.springframework.security.authorization.AuthorizationDecision(true)
								: new org.springframework.security.authorization.AuthorizationDecision(false))
						.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/users/me/onboarding").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/users/workers").hasAuthority(ownerAuthority)
						.requestMatchers("/api/users/**").hasAuthority(ownerAuthority)
						.requestMatchers("/api/affiliates/**").hasAuthority(affiliateAuthority)
						.requestMatchers(HttpMethod.GET, "/api/privileges/**").authenticated()
						.requestMatchers("/api/privileges/**").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.GET, "/api/billing/plans").permitAll()
						.requestMatchers("/api/billing/**").hasAuthority(ownerAuthority)
						.requestMatchers("/api/admin/**").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.POST, "/api/products/*/barcodes")
						.hasAuthority(PrivilegeRole.Worker.name())
						.requestMatchers(HttpMethod.POST, "/api/products/*/submit-approval")
						.hasAuthority(PrivilegeRole.Worker.name())
						.requestMatchers(HttpMethod.PATCH, "/api/products/*/quantity").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.POST, "/api/products").hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/barcodes", "/api/barcodes/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/barcodes", "/api/barcodes/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/transactions/withdrawals", "/api/transactions/withdrawals/**")
						.hasAuthority(ownerAuthority)
						.requestMatchers(HttpMethod.POST, "/api/transactions", "/api/transactions/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/transactions", "/api/transactions/**").hasAuthority(ownerAuthority)
						.requestMatchers("/api/reports", "/api/reports/**").hasAuthority(ownerAuthority)
						.requestMatchers("/api/audit-logs", "/api/audit-logs/**").hasAuthority(ownerAuthority)
						.requestMatchers("/api/tips", "/api/tips/**").hasAuthority(ownerAuthority)
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
						.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.addFilterAfter(
						new RateLimitingFilter(rateLimitService, businessAccessService, objectMapper),
						BearerTokenAuthenticationFilter.class)
				.addFilterAfter(new BusinessAccessFilter(businessAccessService), RateLimitingFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String jwtSecret) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecret.getBytes(StandardCharsets.UTF_8)));
	}

	@Bean
	JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String jwtSecret) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey(jwtSecret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> authoritiesFromRoles(jwt.getClaimAsStringList("roles")));
		return converter;
	}

	private Collection<GrantedAuthority> authoritiesFromRoles(List<String> roles) {
		if (roles == null) {
			return List.of();
		}

		return roles.stream()
				.map(SimpleGrantedAuthority::new)
				.map(GrantedAuthority.class::cast)
				.toList();
	}

	private SecretKey jwtSecretKey(String jwtSecret) {
		return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
