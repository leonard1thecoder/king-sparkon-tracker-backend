package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.UserResponse;
import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Users", description = "Owner and worker user management.")
public class UserController {

	private final TrackerUserService userService;

	public UserController(TrackerUserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@Operation(summary = "List users", description = "Owner-only endpoint for viewing owners and workers.")
	public PageResponse<UserResponse> listUsers(
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size from 1 to 100.") @RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) Principal principal) {
		return PageResponse.from(userService.listUsers(pageable(page, size), principal.getName()), UserResponse::from);
	}

	@PostMapping("/workers")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create worker", description = "Owner-only endpoint for adding one of the allowed worker accounts.")
	public UserResponse createWorker(
			@Valid @RequestBody CreateWorkerRequest request,
			@Parameter(hidden = true) Principal principal) {
		return UserResponse.from(userService.createWorker(request, principal.getName()));
	}

	@DeleteMapping("/workers/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(
			summary = "Delete worker",
			description = "Owner-only endpoint for deleting a worker from the authenticated owner's business."
	)
	public void deleteWorker(
			@Parameter(description = "Worker user id.") @PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		userService.deleteWorker(id, principal.getName());
	}

	@GetMapping("/me")
	@Operation(summary = "Current user", description = "Returns the authenticated user's profile and privilege.")
	public UserResponse currentUser(@Parameter(hidden = true) Principal principal) {
		return UserResponse.from(userService.getUserByUsername(principal.getName()));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get user by id", description = "Owner-only endpoint for viewing one user.")
	public UserResponse getUserById(
			@Parameter(description = "User id.") @PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return UserResponse.from(userService.getUserById(id, principal.getName()));
	}

	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}