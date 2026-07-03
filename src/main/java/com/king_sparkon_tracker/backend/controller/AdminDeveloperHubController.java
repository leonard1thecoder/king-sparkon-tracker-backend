package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.DeveloperHubMetricsResponse;
import com.king_sparkon_tracker.backend.dto.SoftwareDevelopmentRequestResponse;
import com.king_sparkon_tracker.backend.dto.UpdateSoftwareDevelopmentStageRequest;
import com.king_sparkon_tracker.backend.service.DeveloperHubService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/developer-hub")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Admin Developer Hub", description = "Admin delivery endpoints for King Sparkon Dev Hub.")
public class AdminDeveloperHubController {

	private final DeveloperHubService developerHubService;

	public AdminDeveloperHubController(DeveloperHubService developerHubService) {
		this.developerHubService = developerHubService;
	}

	@GetMapping("/software-requests")
	@Operation(summary = "List all software requests")
	public List<SoftwareDevelopmentRequestResponse> listRequests(@Parameter(hidden = true) Principal principal) {
		return developerHubService.listAdminRequests(principal.getName()).stream()
				.map(SoftwareDevelopmentRequestResponse::from)
				.toList();
	}

	@GetMapping("/software-requests/metrics")
	@Operation(summary = "Admin Developer Hub metrics")
	public DeveloperHubMetricsResponse adminMetrics(@Parameter(hidden = true) Principal principal) {
		return developerHubService.adminMetrics(principal.getName());
	}

	@PatchMapping("/software-requests/{requestId}/stage")
	@Operation(summary = "Move a software request to another stage")
	public SoftwareDevelopmentRequestResponse updateStage(
			@PathVariable Long requestId,
			@Valid @RequestBody UpdateSoftwareDevelopmentStageRequest request,
			@Parameter(hidden = true) Principal principal) {
		return SoftwareDevelopmentRequestResponse.from(developerHubService.updateStage(requestId, request, principal.getName()));
	}
}
