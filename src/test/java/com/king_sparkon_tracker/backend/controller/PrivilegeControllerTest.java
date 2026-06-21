package com.king_sparkon_tracker.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.PrivilegeService;

class PrivilegeControllerTest {

	private PrivilegeService privilegeService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		privilegeService = mock(PrivilegeService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new PrivilegeController(privilegeService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void listPrivilegesReturnsRoles() throws Exception {
		when(privilegeService.listPrivileges()).thenReturn(List.of(
				new Privilege(PrivilegeRole.Owner),
				new Privilege(PrivilegeRole.Worker)));

		mockMvc.perform(get("/api/privileges"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Owner"))
				.andExpect(jsonPath("$[1].name").value("Worker"));
	}

	@Test
	void getPrivilegeReturnsRole() throws Exception {
		when(privilegeService.getPrivilege(PrivilegeRole.Owner))
				.thenReturn(new Privilege(PrivilegeRole.Owner));

		mockMvc.perform(get("/api/privileges/Owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Owner"));
	}

	@Test
	void getPrivilegeMapsMissingRoleToNotFound() throws Exception {
		when(privilegeService.getPrivilege(PrivilegeRole.Owner))
				.thenThrow(new ResourceNotFoundException("Privilege not found: Owner"));

		mockMvc.perform(get("/api/privileges/Owner"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Privilege not found: Owner"));
	}

	@Test
	void createPrivilegeCreatesRole() throws Exception {
		when(privilegeService.createPrivilege(any(PrivilegeRole.class)))
				.thenReturn(new Privilege(PrivilegeRole.Owner));

		mockMvc.perform(post("/api/privileges")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Owner"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Owner"));
	}
}
