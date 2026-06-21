package com.king_sparkon_tracker.backend.controller;

import com.king_sparkon_tracker.backend.dto.*;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.security.JwtService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Owner business registration, JWT login, and password recovery.")
public class AuthenticationController {

	private final TrackerUserService userService;
	private final JwtService jwtService;
	private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;


	public AuthenticationController(
			TrackerUserService userService,
			JwtService jwtService,
			PasswordResetService passwordResetService
            ,EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
		this.userService = userService;
		this.jwtService = jwtService;
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register owner", description = "Creates an owner account and links it to a new business.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Owner registered"),
			@ApiResponse(responseCode = "400", description = "Invalid registration details"),
			@ApiResponse(responseCode = "409", description = "Username or email already exists")
	})
	public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
		return UserResponse.from(userService.registerOwner(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticates a registered owner or worker and returns a JWT access token.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "400", description = "Invalid login payload"),
			@ApiResponse(responseCode = "401", description = "Invalid username or password")
	})
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		TrackerUser user = userService.authenticate(request);
		JwtService.TokenResult token = jwtService.generateToken(user);
		return new AuthResponse("Bearer", token.token(), token.expiresAt(), UserResponse.from(user));
	}

	@PostMapping("/forgot-password")
	@Operation(summary = "Forgot password", description = "Sends a password reset email when the account exists.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Request accepted"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public MessageResponse forgotPassword(
			@Valid @RequestBody ForgotPasswordRequest request,
			HttpServletRequest servletRequest,
			@RequestHeader(value = "User-Agent", required = false) String userAgent) {
		passwordResetService.requestPasswordReset(
				request,
				clientIp(servletRequest),
				userAgent);

		return MessageResponse.of("If the email address exists, a password reset link has been sent.");
	}

	@PostMapping("/reset-password")
	@Operation(summary = "Reset password", description = "Resets the user password using a valid reset token.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Password reset successful"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Invalid or expired reset token")
	})
	public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		passwordResetService.resetPassword(request);
		return MessageResponse.of("Password reset successful. You can now login with your new password.");
	}

    @GetMapping("/verify-email")
    public MessageResponse verifyEmail(@RequestParam("token") String token) {
        emailVerificationService.verifyEmail(token);
        return MessageResponse.of("Email verified successfully. You can now login.");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request,
            HttpServletRequest servletRequest,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        emailVerificationService.resendVerificationEmail(request, clientIp(servletRequest), userAgent);
        return MessageResponse.of("If the email address exists and is not verified, a verification link has been sent.");
    }


    private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}