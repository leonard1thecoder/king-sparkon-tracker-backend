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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.king_sparkon_tracker.backend.dto.ResendEmailVerificationRequest;
import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.EmailVerificationToken;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.EmailVerificationTokenRepository;

@Service
@Transactional
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TrackerUserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final AppEmailService appEmailService;
    private final String emailVerificationUrl;
    private final long expirationHours;

    public EmailVerificationService(
            TrackerUserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            AppEmailService appEmailService,
            @Value("${app.frontend.email-verification-url}") String emailVerificationUrl,
            @Value("${app.email-verification.expiration-hours:24}") long expirationHours) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.appEmailService = appEmailService;
        this.emailVerificationUrl = emailVerificationUrl;
        this.expirationHours = expirationHours;
    }

    public void sendVerificationEmail(TrackerUser user, String requestedIp, String userAgent) {
        if (user.isEmailVerified()) {
            log.info("email_verification_skipped userId={} reason=already_verified", user.getId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidateUnusedTokensForUser(user.getId(), now);

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = now.plusHours(expirationHours);

        EmailVerificationToken token = new EmailVerificationToken(
                user,
                tokenHash,
                expiresAt,
                safeTrim(requestedIp, 64),
                safeTrim(userAgent, 512));

        tokenRepository.save(token);

        String verificationUrl = UriComponentsBuilder
                .fromUriString(emailVerificationUrl)
                .queryParam("token", rawToken)
                .build()
                .toUriString();

        appEmailService.sendEmailVerificationEmail(user.getEmailAddress(), user.getUsername(), verificationUrl, expirationHours);
        log.info("email_verification_token_created userId={} email={}", user.getId(), maskEmail(user.getEmailAddress()));
    }

    public void resendVerificationEmail(ResendEmailVerificationRequest request, String requestedIp, String userAgent) {
        String emailAddress = normalizeEmailAddress(request.emailAddress());
        userRepository.findByEmailAddress(emailAddress)
                .ifPresentOrElse(user -> sendVerificationEmail(user, requestedIp, userAgent),
                        () -> log.warn("email_verification_resend_unknown_email email={}", maskEmail(emailAddress)));
    }

    public void verifyEmail(String rawToken) {
        String token = normalizeRequired(rawToken, "Verification token is required");
        String tokenHash = hashToken(token);

        EmailVerificationToken verificationToken = tokenRepository
                .findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("email_verification_failed reason=invalid_or_expired_token");
                    return new InvalidCredentialsException();
                });

        TrackerUser user = verificationToken.getUser();
        if (!user.isEmailVerified()) {
            user.markEmailVerified();
            userRepository.save(user);
        }

        verificationToken.markUsed();
        tokenRepository.save(verificationToken);
        log.info("email_verification_success userId={} username={}", user.getId(), user.getUsername());
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
            throw new IllegalStateException("Could not hash email verification token", ex);
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
