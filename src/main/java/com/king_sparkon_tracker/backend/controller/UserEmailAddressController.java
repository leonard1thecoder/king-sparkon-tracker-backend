package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AdminUpdateUserEmailAddressRequest;
import com.king_sparkon_tracker.backend.dto.ChangeEmailAddressRequest;
import com.king_sparkon_tracker.backend.dto.UserResponse;
import com.king_sparkon_tracker.backend.service.UserEmailAddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Email Address", description = "Email address update endpoints for authenticated users and admin bootstrap maintenance.")
public class UserEmailAddressController {

	private final UserEmailAddressService userEmailAddressService;

	public UserEmailAddressController(UserEmailAddressService userEmailAddressService) {
		this.userEmailAddressService = userEmailAddressService;
	}

	@PatchMapping("/auth/email-address")
	@Operation(summary = "Change authenticated user email address")
	public UserResponse changeOwnEmailAddress(
			@Valid @RequestBody ChangeEmailAddressRequest request,
			@Parameter(hidden = true) Principal principal) {
		return UserResponse.from(userEmailAddressService.changeOwnEmailAddress(request, principal.getName()));
	}

	@PatchMapping("/admin/users/{userId}/email-address")
	@Operation(summary = "TEMPORARY bootstrap endpoint: admin changes any user email address")
	public UserResponse adminUpdateEmailAddress(
			@PathVariable Long userId,
			@Valid @RequestBody AdminUpdateUserEmailAddressRequest request,
			@Parameter(hidden = true) Principal principal) {
		return UserResponse.from(userEmailAddressService.adminUpdateEmailAddress(userId, request, principal.getName()));
	}
}
