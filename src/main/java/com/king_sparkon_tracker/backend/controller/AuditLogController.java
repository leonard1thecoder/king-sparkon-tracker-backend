package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AuditLogResponse;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.service.AuditLogService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/audit-logs")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Audit Logs", description = "Owner-only timeline of sensitive business events.")
public class AuditLogController {

	private final AuditLogService auditLogService;
	private final TrackerUserService userService;

	public AuditLogController(AuditLogService auditLogService, TrackerUserService userService) {
		this.auditLogService = auditLogService;
		this.userService = userService;
	}

	/**
	 * Lists audit records newest-first so owners can review recent operational changes quickly.
	 */
	@GetMapping
	@Operation(summary = "List audit logs", description = "Owner-only endpoint for paginated audit history.")
	public PageResponse<AuditLogResponse> listAuditLogs(
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size from 1 to 100.") @RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) Principal principal) {
		Long businessId = userService.businessForActor(principal.getName()).getId();
		return PageResponse.from(auditLogService.listAuditLogs(businessId, pageable(page, size)), AuditLogResponse::from);
	}

	/**
	 * Normalizes pagination values before passing them into the repository layer.
	 */
	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}
