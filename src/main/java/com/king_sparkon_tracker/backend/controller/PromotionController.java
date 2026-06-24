package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.CreatePromotionRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.dto.PromotionPriceQuoteResponse;
import com.king_sparkon_tracker.backend.dto.PromotionResponse;
import com.king_sparkon_tracker.backend.service.PromotionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

	private final PromotionService promotionService;

	public PromotionController(PromotionService promotionService) {
		this.promotionService = promotionService;
	}

	@GetMapping("/quote")
	public PromotionPriceQuoteResponse quoteCurrentAudience() {
		return promotionService.quoteCurrentAudience();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PromotionResponse createPromotion(@Valid @RequestBody CreatePromotionRequest request, Principal principal) {
		return PromotionResponse.from(promotionService.createOwnerPromotion(request, principal.getName()));
	}

	@GetMapping
	public PageResponse<PromotionResponse> listOwnerPromotions(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Principal principal) {
		return PageResponse.from(promotionService.listOwnerPromotions(pageable(page, size), principal.getName()), PromotionResponse::from);
	}

	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}
