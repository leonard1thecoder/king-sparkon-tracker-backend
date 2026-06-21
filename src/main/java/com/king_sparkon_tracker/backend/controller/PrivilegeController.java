package com.king_sparkon_tracker.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.PrivilegeRequest;
import com.king_sparkon_tracker.backend.dto.PrivilegeResponse;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.PrivilegeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/privileges")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Privileges", description = "Role catalogue used by owner and worker authorization.")
public class PrivilegeController {

	private final PrivilegeService privilegeService;

	public PrivilegeController(PrivilegeService privilegeService) {
		this.privilegeService = privilegeService;
	}

	/**
	 * Lists all seeded privilege roles so clients can display role names consistently.
	 */
	@GetMapping
	@Operation(summary = "List privileges", description = "Returns available application roles.")
	public List<PrivilegeResponse> listPrivileges() {
		return privilegeService.listPrivileges().stream()
				.map(PrivilegeResponse::from)
				.toList();
	}

	/**
	 * Looks up a single privilege by enum role name.
	 */
	@GetMapping("/{role}")
	@Operation(summary = "Get privilege", description = "Returns one privilege by role name.")
	public PrivilegeResponse getPrivilege(@Parameter(description = "Privilege role name.") @PathVariable PrivilegeRole role) {
		return PrivilegeResponse.from(privilegeService.getPrivilege(role));
	}

	/**
	 * Creates a privilege idempotently when administrative setup needs to repair missing role data.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create privilege", description = "Owner-only endpoint that creates a missing privilege role.")
	public PrivilegeResponse createPrivilege(@Valid @RequestBody PrivilegeRequest request) {
		return PrivilegeResponse.from(privilegeService.createPrivilege(request.name()));
	}
}
