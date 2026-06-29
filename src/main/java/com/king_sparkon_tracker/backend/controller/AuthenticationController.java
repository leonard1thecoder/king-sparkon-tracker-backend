package com.king_sparkon_tracker.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.king_sparkon_tracker.backend.dto.*;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.PublicUserRegistrationService;
import com.king_sparkon_tracker.backend.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.king_sparkon_tracker.backend.model.TrackerUser;
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
@Tag(name = "Authentication", description = "Privilege-aware registration, owner business registration, administrator registration, JWT login, refresh-token rotation, and account recovery.")
public class AuthenticationController {

	private static final String DEFAULT_BUSINESS_URL_TEMPLATE = "http://localhost:3000/businesses/{businessId}";

	private final TrackerUserService userService;
	private final RefreshTokenService refreshTokenService;
	private final PasswordResetService passwordResetService;
	private final EmailVerificationService emailVerificationService;
	@Autowired(required = false)
	private BusinessRepository businessRepository;
	@Autowired(required = false)
	private PublicUserRegistrationService publicUserRegistrationService;
	private String businessUrlTemplate = DEFAULT_BUSINESS_URL_TEMPLATE;

	public AuthenticationController(
			TrackerUserService userService,
			RefreshTokenService refreshTokenService,
			PasswordResetService passwordResetService,
			EmailVerificationService emailVerificationService) {
		this.emailVerificationService = emailVerificationService;
		this.userService = userService;
		this.refreshTokenService = refreshTokenService;
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register by selected privilege", description = "Creates a User, Owner business workspace, or Affiliate account from the registration-page serviceRegisteringFor selector.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Account registered"),
			@ApiResponse(responseCode = "400", description = "Invalid registration details"),
			@ApiResponse(responseCode = "409", description = "Username or email already exists")
	})
	public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
		TrackerUser registeredUser = switch (request.serviceRegisteringFor()) {
			case USER -> registerPublicUser(request);
			case BUSINESS_OWNER -> registerOwnerAndAssignBusinessQr(request);
			case AFFILIATE -> userService.registerAffiliate(new RegisterAffiliateRequest(
					request.username(),
					request.emailAddress(),
					request.password(),
					request.localizationCountry(),
					request.physicalAddress(),
					request.cellphoneNumber(),
					request.paypalLink()));
		};

		return UserResponse.from(registeredUser);
	}

	@PostMapping("/register-admin")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register administrator", description = "Creates the single platform administrator account. The email address must end with @kingsparkon.com and onboarding address details are required.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Administrator registered"),
			@ApiResponse(responseCode = "400", description = "Invalid administrator registration details"),
			@ApiResponse(responseCode = "409", description = "Administrator already exists, username already exists, or email already exists")
	})
	public UserResponse registerAdministrator(@Valid @RequestBody RegisterAdministratorRequest request) {
		return UserResponse.from(userService.registerAdministrator(request));
	}

	@PostMapping("/register-affiliate")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register affiliate", description = "Creates an affiliate account with a promotion code and QR link.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Affiliate registered"),
			@ApiResponse(responseCode = "400", description = "Invalid registration details"),
			@ApiResponse(responseCode = "409", description = "Username or email already exists")
	})
	public UserResponse registerAffiliate(@Valid @RequestBody RegisterAffiliateRequest request) {
		return UserResponse.from(userService.registerAffiliate(request));
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticates a registered administrator, owner, affiliate, user, or worker and returns access plus refresh tokens.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "400", description = "Invalid login payload"),
			@ApiResponse(responseCode = "401", description = "Invalid username or password")
	})
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		TrackerUser user = userService.authenticate(request);
		RefreshTokenService.TokenPair pair = refreshTokenService.issueTokenPair(user, clientIp(servletRequest), userAgent);
		return authResponse(pair);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token", description = "Rotates a refresh token and returns a fresh access token pair.")
	public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		return authResponse(refreshTokenService.rotate(request.refreshToken(), clientIp(servletRequest), userAgent));
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout", description = "Revokes the supplied refresh token.")
	public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
		refreshTokenService.revoke(request.refreshToken());
		return MessageResponse.of("Logged out successfully.");
	}

	@PostMapping("/forgot-password")
	@Operation(summary = "Forgot password", description = "Sends a password reset email when the account exists.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Request accepted"),
			@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		passwordResetService.requestPasswordReset(request, clientIp(servletRequest), userAgent);
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
	public MessageResponse resendVerification(@Valid @RequestBody ResendEmailVerificationRequest request, HttpServletRequest servletRequest, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
		emailVerificationService.resendVerificationEmail(request, clientIp(servletRequest), userAgent);
		return MessageResponse.of("If the email address exists and is not verified, a verification link has been sent.");
	}

	private TrackerUser registerPublicUser(RegisterUserRequest request) {
		if (publicUserRegistrationService == null) {
			throw new IllegalStateException("Public user registration service is not available");
		}
		return publicUserRegistrationService.registerUser(request);
	}

	private TrackerUser registerOwnerAndAssignBusinessQr(RegisterUserRequest request) {
		TrackerUser owner = userService.registerOwner(request);
		if (businessRepository != null && owner.getBusiness() != null) {
			Business business = owner.getBusiness();
			if (StringUtils.hasText(request.businessDescription())) {
				business.setDescription(request.businessDescription().trim());
			}
			if (business.getId() != null) {
				business.assignQrCodeUrl(qrCodeUrl(businessPageUrl(business)));
			}
			businessRepository.save(business);
		}
		return owner;
	}

	private String businessPageUrl(Business business) {
		String template = StringUtils.hasText(businessUrlTemplate) ? businessUrlTemplate : DEFAULT_BUSINESS_URL_TEMPLATE;
		return template
				.replace("{businessId}", String.valueOf(business.getId()))
				.replace("{businessName}", URLEncoder.encode(business.getName(), StandardCharsets.UTF_8));
	}

	private String qrCodeUrl(String targetUrl) {
		String encodedUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
		return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=%s".formatted(encodedUrl);
	}

	private AuthResponse authResponse(RefreshTokenService.TokenPair pair) {
		return new AuthResponse("Bearer", pair.accessToken(), pair.accessTokenExpiresAt(), pair.refreshToken(), pair.refreshTokenExpiresAt(), UserResponse.from(pair.user()));
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
