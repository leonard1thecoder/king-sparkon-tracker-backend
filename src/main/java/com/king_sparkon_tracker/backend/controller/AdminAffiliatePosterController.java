package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AffiliatePosterResponse;
import com.king_sparkon_tracker.backend.model.AffiliatePosterCategory;
import com.king_sparkon_tracker.backend.service.AffiliatePosterService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin/affiliate-posters")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminAffiliatePosterController {

	private final AffiliatePosterService posterService;

	public AdminAffiliatePosterController(AffiliatePosterService posterService) {
		this.posterService = posterService;
	}

	@GetMapping
	public List<AffiliatePosterResponse> list(Principal principal) {
		return posterService.listAdmin(principal.getName());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public AffiliatePosterResponse upload(
			@RequestParam AffiliatePosterCategory category,
			@RequestParam String title,
			@RequestParam(required = false) String description,
			@RequestParam("file") MultipartFile file,
			Principal principal) {
		return posterService.upload(category, title, description, file, principal.getName());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id, Principal principal) {
		posterService.deactivate(id, principal.getName());
	}
}
