package com.king_sparkon_tracker.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.dto.ForgotPasswordRequest;
import com.king_sparkon_tracker.backend.dto.LoginRequest;
import com.king_sparkon_tracker.backend.dto.RefreshTokenRequest;
import com.king_sparkon_tracker.backend.dto.RegisterAffiliateRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.dto.ResendEmailVerificationRequest;
import com.king_sparkon_tracker.backend.dto.ResetPasswordRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.exception.DuplicateUsernameException;
import com.king_sparkon_tracker.backend.exception.EmailNotVerifiedException;
import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;
import com.king_sparkon_tracker.backend.service.RefreshTokenService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

class AuthenticationControllerTest {

	private TrackerUserService userService;
	private RefreshTokenService refreshTokenService;
	private PasswordResetService passwordResetService;
	private EmailVerificationService emailVerificationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userService = mock(TrackerUserService.class);
		refreshTokenService = mock(RefreshTokenService.class);
		passwordResetService = mock(PasswordResetService.class);
		emailVerificationService = mock(EmailVerificationService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new AuthenticationController(userService, refreshTokenService, passwordResetService, emailVerificationService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void registerCreatesOwner() throws Exception {
		when(userService.registerOwner(any(RegisterUserRequest.class)))
				.thenReturn(user("alice", "alice@kingsparkon.co.za", PrivilegeRole.Owner));

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "alice",
								  "emailAddress": "alice@kingsparkon.co.za",
								  "password": "secret",
								  "businessName": "Alice Traders",
								  "physicalAddress": "12 Main Road",
								  "cellphoneNumber": "+27821234567"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.emailAddress").value("alice@kingsparkon.co.za"))
				.andExpect(jsonPath("$.privilege").value("Owner"));
	}

	@Test
	void registerMapsDuplicateUsernameToConflict() throws Exception {
		when(userService.registerOwner(any(RegisterUserRequest.class)))
				.thenThrow(new DuplicateUsernameException("alice"));

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "alice",
								  "emailAddress": "alice@kingsparkon.co.za",
								  "password": "secret",
								  "businessName": "Alice Traders"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Username already exists: alice"));
	}

	@Test
	void loginAuthenticatesUser() throws Exception {
		TrackerUser owner = user("owner", PrivilegeRole.Owner);

		when(userService.authenticate(any(LoginRequest.class))).thenReturn(owner);
		when(refreshTokenService.issueTokenPair(eq(owner), eq("127.0.0.1"), any()))
				.thenReturn(new RefreshTokenService.TokenPair(
						"jwt-token",
						Instant.parse("2026-01-01T00:00:00Z"),
						"refresh-token",
						Instant.parse("2026-02-01T00:00:00Z"),
						owner));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "owner",
								  "password": "secret"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").value("jwt-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.user.username").value("owner"))
				.andExpect(jsonPath("$.user.privilege").value("Owner"))
				.andExpect(jsonPath("$.user.onboardingRequired").value(true));
	}

	@Test
	void refreshRotatesRefreshToken() throws Exception {
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		when(refreshTokenService.rotate(eq("old-refresh"), eq("127.0.0.1"), any()))
				.thenReturn(new RefreshTokenService.TokenPair(
						"new-jwt-token",
						Instant.parse("2026-01-01T00:00:00Z"),
						"new-refresh-token",
						Instant.parse("2026-02-01T00:00:00Z"),
						owner));

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "old-refresh"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("new-jwt-token"))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "refreshToken": "refresh-token"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Logged out successfully."));

		verify(refreshTokenService).revoke("refresh-token");
	}

	@Test
	void loginMapsInvalidCredentialsToUnauthorized() throws Exception {
		when(userService.authenticate(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "owner",
								  "password": "wrong"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void forgotPasswordAcceptsRequest() throws Exception {
		mockMvc.perform(post("/api/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.header("User-Agent", "JUnit")
						.header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
						.content("""
								{
								  "emailAddress": "owner@example.com"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message")
						.value("If the email address exists, a password reset link has been sent."));

		verify(passwordResetService).requestPasswordReset(
				any(ForgotPasswordRequest.class),
				eq("10.0.0.1"),
				eq("JUnit"));
	}

	@Test
	void forgotPasswordRejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "emailAddress": "bad-email"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void resetPasswordAcceptsValidToken() throws Exception {
		mockMvc.perform(post("/api/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "reset-token",
								  "newPassword": "NewPassword123!",
								  "confirmPassword": "NewPassword123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message")
						.value("Password reset successful. You can now login with your new password."));

		verify(passwordResetService).resetPassword(any(ResetPasswordRequest.class));
	}

	@Test
	void resetPasswordMapsInvalidTokenToUnauthorized() throws Exception {
		doThrow(new InvalidCredentialsException())
				.when(passwordResetService)
				.resetPassword(any(ResetPasswordRequest.class));

		mockMvc.perform(post("/api/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "bad-token",
								  "newPassword": "NewPassword123!",
								  "confirmPassword": "NewPassword123!"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void resetPasswordRejectsBlankToken() throws Exception {
		mockMvc.perform(post("/api/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "",
								  "newPassword": "NewPassword123!",
								  "confirmPassword": "NewPassword123!"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void resetPasswordRejectsShortPassword() throws Exception {
		mockMvc.perform(post("/api/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "token": "reset-token",
								  "newPassword": "short",
								  "confirmPassword": "short"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginMapsEmailNotVerifiedToForbidden() throws Exception {
		when(userService.authenticate(any(LoginRequest.class))).thenThrow(new EmailNotVerifiedException());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "owner",
								  "password": "secret"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Email address is not verified. Please verify your email before logging in."));
	}

	@Test
	void verifyEmailAcceptsToken() throws Exception {
		mockMvc.perform(get("/api/auth/verify-email").param("token", "valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Email verified successfully. You can now login."));

		verify(emailVerificationService).verifyEmail("valid-token");
	}

	@Test
	void verifyEmailMapsInvalidTokenToUnauthorized() throws Exception {
		doThrow(new InvalidCredentialsException()).when(emailVerificationService).verifyEmail("bad-token");

		mockMvc.perform(get("/api/auth/verify-email").param("token", "bad-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void resendVerificationAcceptsRequest() throws Exception {
		mockMvc.perform(post("/api/auth/resend-verification")
						.contentType(MediaType.APPLICATION_JSON)
						.header("User-Agent", "JUnit")
						.header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
						.content("""
								{
								  "emailAddress": "owner@example.com"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("If the email address exists and is not verified, a verification link has been sent."));

		verify(emailVerificationService).resendVerificationEmail(any(ResendEmailVerificationRequest.class), eq("10.0.0.1"), eq("JUnit"));
	}

	private TrackerUser user(String username, PrivilegeRole role) {
		return new TrackerUser(username, username + "@example.com", "encoded", new Privilege(role));
	}

	private TrackerUser user(String username, String emailAddress, PrivilegeRole role) {
		return new TrackerUser(username, emailAddress, "encoded", new Privilege(role));
	}
}
