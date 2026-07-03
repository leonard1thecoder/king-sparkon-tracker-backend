package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.UpdateUserEmailVerificationStatusRequest;
import com.king_sparkon_tracker.backend.dto.UserEmailVerificationStatusResponse;
import com.king_sparkon_tracker.backend.service.UserEmailVerificationStatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Admin User Verification", description = "Admin endpoints for manually updating user verification status.")
public class UserVerificationStatusController {

	private final UserEmailVerificationStatusService userEmailVerificationStatusService;

	public UserVerificationStatusController(UserEmailVerificationStatusService userEmailVerificationStatusService) {
		this.userEmailVerificationStatusService = userEmailVerificationStatusService;
	}

	@PatchMapping("/{userId}/email-verification-status")
	@Operation(summary = "Update a user's email verification status")
	public UserEmailVerificationStatusResponse updateEmailVerificationStatus(
			@PathVariable Long userId,
			@Valid @RequestBody UpdateUserEmailVerificationStatusRequest request,
			@Parameter(hidden = true) Principal principal) {
		return userEmailVerificationStatusService.updateEmailVerificationStatus(userId, request, principal.getName());
	}
}
