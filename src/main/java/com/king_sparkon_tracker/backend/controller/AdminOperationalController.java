package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AdminOverviewResponse;
import com.king_sparkon_tracker.backend.dto.AuditLogResponse;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.service.AdministratorService;
import com.king_sparkon_tracker.backend.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Administration", description = "Platform administrator operational reporting APIs.")
public class AdminOperationalController {

	private final AdministratorService administratorService;
	private final AuditLogService auditLogService;

	public AdminOperationalController(
			AdministratorService administratorService,
			AuditLogService auditLogService) {
		this.administratorService = administratorService;
		this.auditLogService = auditLogService;
	}

	@GetMapping("/reports/overview")
	@Operation(summary = "Admin report overview", description = "Returns platform totals used by the administrator reports workspace.")
	public AdminOverviewResponse reportOverview(@Parameter(hidden = true) Principal principal) {
		return administratorService.overview(principal.getName());
	}

	@GetMapping("/audit-logs")
	@Operation(summary = "List platform audit logs", description = "Returns the newest platform audit events for administrators.")
	public PageResponse<AuditLogResponse> auditLogs(
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size from 1 to 100.") @RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(auditLogService.listAuditLogs(pageable(page, size)), AuditLogResponse::from);
	}

	private Pageable pageable(int page, int size) {
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
	}
}
