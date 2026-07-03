package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.CreateSoftwareDevelopmentRequest;
import com.king_sparkon_tracker.backend.dto.DeveloperHubMetricsResponse;
import com.king_sparkon_tracker.backend.dto.SoftwareDevelopmentRequestResponse;
import com.king_sparkon_tracker.backend.service.DeveloperHubService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/developer-hub")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Developer Hub", description = "Owner software request endpoints for King Sparkon Dev Hub.")
public class DeveloperHubController {

	private final DeveloperHubService developerHubService;

	public DeveloperHubController(DeveloperHubService developerHubService) {
		this.developerHubService = developerHubService;
	}

	@GetMapping("/software-requests")
	@Operation(summary = "List owner software requests")
	public List<SoftwareDevelopmentRequestResponse> listOwnerRequests(@Parameter(hidden = true) Principal principal) {
		return developerHubService.listOwnerRequests(principal.getName()).stream()
				.map(SoftwareDevelopmentRequestResponse::from)
				.toList();
	}

	@PostMapping("/software-requests")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create owner software request")
	public SoftwareDevelopmentRequestResponse createSoftwareRequest(
			@Valid @RequestBody CreateSoftwareDevelopmentRequest request,
			@Parameter(hidden = true) Principal principal) {
		return SoftwareDevelopmentRequestResponse.from(developerHubService.createSoftwareRequest(request, principal.getName()));
	}

	@GetMapping("/software-requests/metrics")
	@Operation(summary = "Owner Developer Hub metrics")
	public DeveloperHubMetricsResponse ownerMetrics(@Parameter(hidden = true) Principal principal) {
		return developerHubService.ownerMetrics(principal.getName());
	}
}
