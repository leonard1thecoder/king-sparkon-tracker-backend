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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.ForgotPasswordRequest;
import com.king_sparkon_tracker.backend.dto.ResetPasswordRequest;
import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.model.PasswordResetToken;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.PasswordResetTokenRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	@Mock
	private PasswordResetTokenRepository tokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AppEmailService appEmailService;

	@Mock
	private RefreshTokenService refreshTokenService;

	private PasswordResetService passwordResetService;

	@BeforeEach
	void setUp() {
		passwordResetService = new PasswordResetService(
				userRepository,
				tokenRepository,
				passwordEncoder,
				appEmailService,
				refreshTokenService,
				"http://localhost:3000/reset-password",
				30);
	}

	@Test
	void requestPasswordResetCreatesTokenAndSendsEmailWhenUserExists() {
		TrackerUser user = user(10L, "owner", "owner@example.com", "encoded-old-password");

		when(userRepository.findByEmailAddress("owner@example.com")).thenReturn(Optional.of(user));
		when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		passwordResetService.requestPasswordReset(
				new ForgotPasswordRequest(" OWNER@EXAMPLE.COM "),
				"127.0.0.1",
				"JUnit");

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

		verify(tokenRepository).invalidateUnusedTokensForUser(eq(10L), any(LocalDateTime.class));
		verify(tokenRepository).save(tokenCaptor.capture());

		PasswordResetToken savedToken = tokenCaptor.getValue();

		assertThat(savedToken.getUser()).isSameAs(user);
		assertThat(savedToken.getTokenHash()).isNotBlank();
		assertThat(savedToken.getTokenHash()).hasSize(64);
		assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(20));
		assertThat(savedToken.getRequestedIp()).isEqualTo("127.0.0.1");
		assertThat(savedToken.getUserAgent()).isEqualTo("JUnit");

		verify(appEmailService).sendPasswordResetEmail(
				argThat(email -> email.equals("owner@example.com")),
				argThat(username -> username.equals("owner")),
				argThat(resetUrl -> resetUrl.startsWith("http://localhost:3000/reset-password?token=")),
				eq(30L));
	}

	@Test
	void requestPasswordResetDoesNotLeakUnknownEmail() {
		when(userRepository.findByEmailAddress("missing@example.com")).thenReturn(Optional.empty());

		passwordResetService.requestPasswordReset(
				new ForgotPasswordRequest(" missing@example.com "),
				"127.0.0.1",
				"JUnit");

		verify(tokenRepository, never()).save(any());
		verify(tokenRepository, never()).invalidateUnusedTokensForUser(anyLong(), any(LocalDateTime.class));
		verify(appEmailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyLong());
	}

	@Test
	void resetPasswordUpdatesPasswordMarksTokenUsedAndRevokesRefreshTokens() {
		String rawToken = "valid-reset-token";
		String tokenHash = sha256(rawToken);

		TrackerUser user = user(20L, "worker", "worker@example.com", "encoded-old-password");
		PasswordResetToken token = new PasswordResetToken(
				user,
				tokenHash,
				LocalDateTime.now().plusMinutes(30),
				"127.0.0.1",
				"JUnit");

		when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
				argThat(hash -> hash.equals(tokenHash)),
				any(LocalDateTime.class)))
				.thenReturn(Optional.of(token));

		when(passwordEncoder.encode("NewPassword123!")).thenReturn("encoded-new-password");

		passwordResetService.resetPassword(
				new ResetPasswordRequest(rawToken, "NewPassword123!", "NewPassword123!"));

		assertThat(user.getPassword()).isEqualTo("encoded-new-password");
		assertThat(token.getUsedAt()).isNotNull();

		verify(userRepository).save(user);
		verify(tokenRepository).save(token);
		verify(refreshTokenService).revokeAllForUser(20L);
	}

	@Test
	void resetPasswordRejectsMismatchedPasswords() {
		assertThatThrownBy(() -> passwordResetService.resetPassword(
				new ResetPasswordRequest("token", "NewPassword123!", "DifferentPassword123!")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("New password and confirm password do not match");

		verify(tokenRepository, never()).findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(anyString(), any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void resetPasswordRejectsShortPassword() {
		assertThatThrownBy(() -> passwordResetService.resetPassword(
				new ResetPasswordRequest("token", "short", "short")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("New password must be at least 8 characters");

		verify(tokenRepository, never()).findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(anyString(), any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void resetPasswordRejectsInvalidOrExpiredToken() {
		String rawToken = "expired-or-invalid-token";
		String tokenHash = sha256(rawToken);

		when(tokenRepository.findFirstByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
				argThat(hash -> hash.equals(tokenHash)),
				any(LocalDateTime.class)))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> passwordResetService.resetPassword(
				new ResetPasswordRequest(rawToken, "NewPassword123!", "NewPassword123!")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid username or password");

		verify(userRepository, never()).save(any());
		verify(tokenRepository, never()).save(any());
	}

	@Test
	void requestPasswordResetTrimsLongUserAgentAndIpBeforeSaving() {
		TrackerUser user = user(30L, "owner", "owner@example.com", "encoded-old-password");

		String longIp = "1".repeat(100);
		String longUserAgent = "A".repeat(700);

		when(userRepository.findByEmailAddress("owner@example.com")).thenReturn(Optional.of(user));
		when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		passwordResetService.requestPasswordReset(
				new ForgotPasswordRequest("owner@example.com"),
				longIp,
				longUserAgent);

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(tokenRepository).save(tokenCaptor.capture());

		assertThat(tokenCaptor.getValue().getRequestedIp()).hasSize(64);
		assertThat(tokenCaptor.getValue().getUserAgent()).hasSize(512);
	}

	private TrackerUser user(Long id, String username, String emailAddress, String password) {
		TrackerUser user = new TrackerUser(
				username,
				emailAddress,
				password,
				new Privilege(PrivilegeRole.Owner));

		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not hash test token", ex);
		}
	}
}
