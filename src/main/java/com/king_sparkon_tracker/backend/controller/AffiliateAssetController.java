package com.king_sparkon_tracker.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AffiliatePosterResponse;
import com.king_sparkon_tracker.backend.service.AffiliatePosterService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/affiliates/assets")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AffiliateAssetController {

	private final AffiliatePosterService posterService;

	public AffiliateAssetController(AffiliatePosterService posterService) {
		this.posterService = posterService;
	}

	@GetMapping
	public List<AffiliatePosterResponse> list() {
		return posterService.listActive();
	}
}
