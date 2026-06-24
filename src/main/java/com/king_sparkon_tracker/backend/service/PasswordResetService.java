package com.king_sparkon_tracker.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.king_sparkon_tracker.backend.dto.ForgotPasswordRequest;
import com.king_sparkon_tracker.backend.dto.ResetPasswordRequest;
import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.model.PasswordResetToken;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.PasswordResetTokenRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final TrackerUserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppEmailService appEmailService;
	private final RefreshTokenService refreshTokenService;
	private final String resetPasswordUrl;
	private final long expirationMinutes;

	public PasswordResetService(
			TrackerUserRepository userRepository,
			PasswordResetTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder,
			AppEmailService appEmailService,
			RefreshTokenService refreshTokenService,
			@Value("${app.frontend.reset-password-url}") String resetPasswordUrl,
			@Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.appEmailService = appEmailService;
		this.refreshTokenService = refreshTokenService;
		this.resetPasswordUrl = resetPasswordUrl;
		this.expirationMinutes = expirationMinutes;
	}

	public void requestPasswordReset(
			ForgotPasswordRequest request,
			String requestedIp,
			String userAgent) {
		String emailAddress = normalizeEmailAddress(request.emailAddress());

		userRepository.findByEmailAddress(emailAddress)
				.ifPresentOrElse(user -> createAndSendResetToken(user, requestedIp, userAgent), () -> {
					log.warn("password_reset_requested_unknown_email email={}", maskEmail(emailAddress));
				});
	}

	public void resetPassword(ResetPasswordRequest request) {
		String token = normalizeRequired(request.token(), "Reset token is required");
		String newPassword = normalizeRequired(request.newPassword(), "New password is required");
		String confirmPassword = normalizeRequired(request.confirmPassword(), "Confirm password is required");

		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("New password and confirm password do not match");
		}

		if (newPassword.length() < 8) {
			throw new IllegalArgumentException("New password must be at least 8 characters");
		}

		String tokenHash = hashToken(token);

		PasswordResetToken resetToken = tokenRepository
				.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
				.orElseThrow(() -> {
					log.warn("password_reset_failed reason=invalid_or_expired_token");
					return new InvalidCredentialsException();
				});

		TrackerUser user = resetToken.getUser();
		user.setPassword(passwordEncoder.encode(newPassword));
		resetToken.markUsed();

		userRepository.save(user);
		tokenRepository.save(resetToken);
		refreshTokenService.revokeAllForUser(user.getId());

		log.info("password_reset_success userId={} username={}", user.getId(), user.getUsername());
	}

	private void createAndSendResetToken(
			TrackerUser user,
			String requestedIp,
			String userAgent) {
		LocalDateTime now = LocalDateTime.now();

		tokenRepository.invalidateUnusedTokensForUser(user.getId(), now);

		String rawToken = generateSecureToken();
		String tokenHash = hashToken(rawToken);
		LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);

		PasswordResetToken token = new PasswordResetToken(
				user,
				tokenHash,
				expiresAt,
				safeTrim(requestedIp, 64),
				safeTrim(userAgent, 512));

		tokenRepository.save(token);

		String resetUrl = UriComponentsBuilder
				.fromUriString(resetPasswordUrl)
				.queryParam("token", rawToken)
				.build()
				.toUriString();

		appEmailService.sendPasswordResetEmail(
				user.getEmailAddress(),
				user.getUsername(),
				resetUrl,
				expirationMinutes);

		log.info("password_reset_token_created userId={} email={}", user.getId(), maskEmail(user.getEmailAddress()));
	}

	private String generateSecureToken() {
		byte[] bytes = new byte[48];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not hash reset token", ex);
		}
	}

	private String normalizeEmailAddress(String emailAddress) {
		return normalizeRequired(emailAddress, "Email address is required").toLowerCase(Locale.ROOT);
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String safeTrim(String value, int maxLength) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}

	private String maskEmail(String email) {
		if (!StringUtils.hasText(email)) {
			return "***";
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}
}
