package com.king_sparkon_tracker.backend.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.service.OnboardingProfileService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

class UserControllerTest {

	private TrackerUserService userService;
	private OnboardingProfileService onboardingProfileService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userService = mock(TrackerUserService.class);
		onboardingProfileService = mock(OnboardingProfileService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new UserController(userService, onboardingProfileService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void listUsersReturnsAllUsers() throws Exception {
		when(userService.listUsers(PageRequest.of(0, 20), "owner")).thenReturn(new PageImpl<>(
				java.util.List.of(
						user("owner", PrivilegeRole.Owner),
						user("worker", PrivilegeRole.Worker)),
				PageRequest.of(0, 20),
				2));

		mockMvc.perform(get("/api/users").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].username").value("owner"))
				.andExpect(jsonPath("$.content[0].emailAddress").value("owner@example.com"))
				.andExpect(jsonPath("$.content[0].privilege").value("Owner"))
				.andExpect(jsonPath("$.content[1].username").value("worker"))
				.andExpect(jsonPath("$.content[1].privilege").value("Worker"))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void createWorkerReturnsCreatedWorker() throws Exception {
		TrackerUser worker = user("worker", PrivilegeRole.Worker);
		worker.updateWorkerProfile("Cashier", true);
		worker.assignTipQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?data=worker");
		when(userService.createWorker(org.mockito.ArgumentMatchers.any(CreateWorkerRequest.class), org.mockito.ArgumentMatchers.eq("owner")))
				.thenReturn(worker);

		mockMvc.perform(post("/api/users/workers")
				.principal(() -> "owner")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "worker",
						  "emailAddress": "worker@example.com",
						  "password": "secret",
						  "jobTitle": "Cashier",
						  "tipQrCodeEnabled": true
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("worker"))
				.andExpect(jsonPath("$.privilege").value("Worker"))
				.andExpect(jsonPath("$.jobTitle").value("Cashier"))
				.andExpect(jsonPath("$.tipQrCodeEnabled").value(true))
				.andExpect(jsonPath("$.tipQrCodeUrl").value("https://api.qrserver.com/v1/create-qr-code/?data=worker"))
				.andExpect(jsonPath("$.onboardingRequired").value(true));
	}

	@Test
	void completeOnboardingReturnsUpdatedCurrentUser() throws Exception {
		TrackerUser worker = user("worker", PrivilegeRole.Worker);
		worker.completeOnboarding("45 Worker Street", "+27821234567");
		when(onboardingProfileService.completeUserOnboarding(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("worker")))
				.thenReturn(worker);

		mockMvc.perform(patch("/api/users/me/onboarding")
						.principal(() -> "worker")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "physicalAddress": "45 Worker Street",
								  "cellphoneNumber": "+27821234567"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.physicalAddress").value("45 Worker Street"))
				.andExpect(jsonPath("$.cellphoneNumber").value("+27821234567"))
				.andExpect(jsonPath("$.onboardingCompleted").value(true))
				.andExpect(jsonPath("$.onboardingRequired").value(false));
	}

	@Test
	void currentUserReturnsAuthenticatedUser() throws Exception {
		when(userService.getUserByUsername("alice")).thenReturn(user("alice", PrivilegeRole.Worker));

		mockMvc.perform(get("/api/users/me").principal(() -> "alice"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.privilege").value("Worker"))
				.andExpect(jsonPath("$.onboardingRequired").value(true));
	}

	@Test
	void getUserByIdReturnsUser() throws Exception {
		when(userService.getUserById(7L, "owner")).thenReturn(user("alice", PrivilegeRole.Worker));

		mockMvc.perform(get("/api/users/7").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice"));
	}

	@Test
	void getUserByIdMapsMissingUserToNotFound() throws Exception {
		when(userService.getUserById(7L, "owner")).thenThrow(new ResourceNotFoundException("User not found: 7"));

		mockMvc.perform(get("/api/users/7").principal(() -> "owner"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("User not found: 7"));
	}

	@Test
	void deleteWorkerReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/users/workers/{id}", 2L)
						.principal(ownerPrincipal()))
				.andExpect(status().isNoContent());

		verify(userService).deleteWorker(2L, "owner");
	}

	@Test
	void deleteWorkerMapsBusinessRuleFailureToBadRequest() throws Exception {
		doThrow(new IllegalArgumentException("Only worker accounts can be deleted"))
				.when(userService)
				.deleteWorker(3L, "owner");

		mockMvc.perform(delete("/api/users/workers/{id}", 3L)
						.principal(ownerPrincipal()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Only worker accounts can be deleted"));
	}

	private Principal ownerPrincipal() {
		return () -> "owner";
	}

	private TrackerUser user(String username, PrivilegeRole role) {
		return new TrackerUser(username, username + "@example.com", "encoded", new Privilege(role));
	}
}
