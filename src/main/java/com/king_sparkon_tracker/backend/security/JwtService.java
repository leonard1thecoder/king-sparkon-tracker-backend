package com.king_sparkon_tracker.backend.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.TrackerUser;

@Service
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);

	private final JwtEncoder jwtEncoder;
	private final long expirationMinutes;

	public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.expirationMinutes = expirationMinutes;
	}

	/**
	 * Issues a signed JWT containing identity and role claims, without logging the token value.
	 */
	public TokenResult generateToken(TrackerUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
		String role = user.getPrivilege().getName().name();
		Business business = user.getBusiness();
		JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
				.issuer("king-sparkon-backend")
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(user.getUsername())
				.claim("userId", user.getId())
				.claim("emailAddress", user.getEmailAddress())
				.claim("roles", List.of(role));

		if (business != null) {
			claimsBuilder
					.claim("businessId", business.getId())
					.claim("businessName", business.getName());
		}

		JwtClaimsSet claims = claimsBuilder.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
		log.info("jwt_issued userId={} username={} role={} expiresAt={}", user.getId(), user.getUsername(), role, expiresAt);
		return new TokenResult(token, expiresAt);
	}

	/**
	 * Carries the raw token to the API response together with its expiration instant.
	 */
	public record TokenResult(String token, Instant expiresAt) {
	}
}