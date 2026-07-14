package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AffiliateLeadResponse;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.service.AffiliateLeadService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/affiliates/leads")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AffiliateLeadController {

	private final AffiliateLeadService leadService;

	public AffiliateLeadController(AffiliateLeadService leadService) {
		this.leadService = leadService;
	}

	@GetMapping
	public PageResponse<AffiliateLeadResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			Principal principal) {
		return leadService.list(page, size, principal.getName());
	}
}
