package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.ResendEmailVerificationRequest;
import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.EmailVerificationToken;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.EmailVerificationTokenRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private TrackerUserRepository userRepository;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private AppEmailService appEmailService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(userRepository, tokenRepository, appEmailService, "http://localhost:3000/verify-email", 24);
    }

    @Test
    void sendVerificationEmailCreatesTokenAndSendsEmailWhenUserIsNotVerified() {
        TrackerUser user = user(1L, "owner", "owner@example.com", false);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.sendVerificationEmail(user, "127.0.0.1", "JUnit");

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).invalidateUnusedTokensForUser(eq(1L), any(LocalDateTime.class));
        verify(tokenRepository).save(tokenCaptor.capture());

        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getRequestedIp()).isEqualTo("127.0.0.1");
        assertThat(savedToken.getUserAgent()).isEqualTo("JUnit");

        verify(appEmailService).sendEmailVerificationEmail(
                argThat(email -> email.equals("owner@example.com")),
                argThat(username -> username.equals("owner")),
                argThat(url -> url.startsWith("http://localhost:3000/verify-email?token=")),
                eq(24L));
    }

    @Test
    void sendVerificationEmailDoesNothingWhenAlreadyVerified() {
        TrackerUser user = user(1L, "owner", "owner@example.com", true);
        emailVerificationService.sendVerificationEmail(user, "127.0.0.1", "JUnit");
        verify(tokenRepository, never()).invalidateUnusedTokensForUser(anyLong(), any(LocalDateTime.class));
        verify(tokenRepository, never()).save(any());
        verify(appEmailService, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void resendVerificationEmailDoesNotLeakUnknownEmail() {
        when(userRepository.findByEmailAddress("missing@example.com")).thenReturn(Optional.empty());
        emailVerificationService.resendVerificationEmail(new ResendEmailVerificationRequest(" MISSING@EXAMPLE.COM "), "127.0.0.1", "JUnit");
        verify(tokenRepository, never()).save(any());
        verify(appEmailService, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void verifyEmailMarksUserVerifiedAndTokenUsed() {
        String rawToken = "valid-email-token";
        String tokenHash = sha256(rawToken);
        TrackerUser user = user(2L, "owner", "owner@example.com", false);
        EmailVerificationToken token = new EmailVerificationToken(user, tokenHash, LocalDateTime.now().plusHours(24), "127.0.0.1", "JUnit");

        when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(argThat(hash -> hash.equals(tokenHash)), any(LocalDateTime.class)))
                .thenReturn(Optional.of(token));

        emailVerificationService.verifyEmail(rawToken);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifyEmailRejectsInvalidOrExpiredToken() {
        String rawToken = "invalid-email-token";
        String tokenHash = sha256(rawToken);
        when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(argThat(hash -> hash.equals(tokenHash)), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(rawToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }

    private TrackerUser user(Long id, String username, String emailAddress, boolean verified) {
        TrackerUser user = new TrackerUser(username, emailAddress, "encoded-password", new Privilege(PrivilegeRole.Owner));
        ReflectionTestUtils.setField(user, "id", id);
        if (verified) user.markEmailVerified();
        return user;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash test token", ex);
        }
    }
}
