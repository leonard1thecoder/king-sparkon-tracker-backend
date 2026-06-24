package com.king_sparkon_tracker.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.model.RefreshToken;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.RefreshTokenRepository;
import com.king_sparkon_tracker.backend.security.JwtService;

@Service
@Transactional
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtService jwtService;
	private final long expirationDays;

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			JwtService jwtService,
			@Value("${app.refresh-token.expiration-days:30}") long expirationDays) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtService = jwtService;
		this.expirationDays = expirationDays;
	}

	public TokenPair issueTokenPair(TrackerUser user, String clientIp, String userAgent) {
		JwtService.TokenResult accessToken = jwtService.generateToken(user);
		IssuedRefreshToken refreshToken = issueRefreshToken(user, clientIp, userAgent);
		return new TokenPair(accessToken.token(), accessToken.expiresAt(), refreshToken.rawToken(), refreshToken.expiresAt(), user);
	}

	public TokenPair rotate(String rawRefreshToken, String clientIp, String userAgent) {
		String currentHash = hashRequired(rawRefreshToken);
		RefreshToken current = refreshTokenRepository.findByTokenHash(currentHash)
				.orElseThrow(this::invalidRefreshToken);
		Instant now = Instant.now();

		if (!current.isActive(now)) {
			log.warn("refresh_token_rejected tokenId={} userId={} reason=inactive", current.getId(), current.getUser().getId());
			throw invalidRefreshToken();
		}

		IssuedRefreshToken replacement = buildRefreshToken(current.getUser(), clientIp, userAgent, now);
		current.rotateTo(replacement.hash(), now);
		refreshTokenRepository.save(current);
		refreshTokenRepository.save(replacement.entity());

		JwtService.TokenResult accessToken = jwtService.generateToken(current.getUser());
		log.info("refresh_token_rotated previousTokenId={} userId={} expiresAt={}", current.getId(), current.getUser().getId(), replacement.expiresAt());
		return new TokenPair(accessToken.token(), accessToken.expiresAt(), replacement.rawToken(), replacement.expiresAt(), current.getUser());
	}

	public void revoke(String rawRefreshToken) {
		String tokenHash = hashRequired(rawRefreshToken);
		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
			if (token.getRevokedAt() == null) {
				token.markRevoked(Instant.now());
				refreshTokenRepository.save(token);
			}
			log.info("refresh_token_revoked tokenId={} userId={}", token.getId(), token.getUser().getId());
		});
	}

	public void revokeAllForUser(Long userId) {
		if (userId == null) {
			return;
		}
		int revoked = refreshTokenRepository.revokeActiveTokensForUser(userId, Instant.now());
		log.info("refresh_tokens_revoked_for_user userId={} revokedCount={}", userId, revoked);
	}

	private IssuedRefreshToken issueRefreshToken(TrackerUser user, String clientIp, String userAgent) {
		IssuedRefreshToken token = buildRefreshToken(user, clientIp, userAgent, Instant.now());
		refreshTokenRepository.save(token.entity());
		log.info("refresh_token_issued userId={} expiresAt={}", user.getId(), token.expiresAt());
		return token;
	}

	private IssuedRefreshToken buildRefreshToken(TrackerUser user, String clientIp, String userAgent, Instant now) {
		String rawToken;
		String tokenHash;
		do {
			rawToken = generateSecureToken();
			tokenHash = hash(rawToken);
		} while (refreshTokenRepository.existsByTokenHash(tokenHash));

		Instant expiresAt = now.plus(expirationDays, ChronoUnit.DAYS);
		RefreshToken entity = new RefreshToken(user, tokenHash, now, expiresAt, safeTrim(clientIp, 64), safeTrim(userAgent, 512));
		return new IssuedRefreshToken(rawToken, tokenHash, expiresAt, entity);
	}

	private InvalidCredentialsException invalidRefreshToken() {
		return new InvalidCredentialsException();
	}

	private String hashRequired(String rawRefreshToken) {
		if (!StringUtils.hasText(rawRefreshToken)) {
			throw invalidRefreshToken();
		}
		return hash(rawRefreshToken.trim());
	}

	private String generateSecureToken() {
		byte[] bytes = new byte[64];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception exception) {
			throw new IllegalStateException("Could not hash refresh token", exception);
		}
	}

	private String safeTrim(String value, int maxLength) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}

	private record IssuedRefreshToken(String rawToken, String hash, Instant expiresAt, RefreshToken entity) {
	}

	public record TokenPair(
			String accessToken,
			Instant accessTokenExpiresAt,
			String refreshToken,
			Instant refreshTokenExpiresAt,
			TrackerUser user) {
	}
}
