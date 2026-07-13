package com.king_sparkon_tracker.backend.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.ProductPromotionResponse;
import com.king_sparkon_tracker.backend.dto.ProductResponse;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductPromotion;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductPromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/product-promotions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Product promotions", description = "Wallet-funded promoted product placement in the user tuck shop.")
public class ProductPromotionController {

	private final ProductPromotionService promotionService;
	private final ProductPricingService productPricingService;
	private final PriceLocalizationService priceLocalizationService;

	public ProductPromotionController(
			ProductPromotionService promotionService,
			ProductPricingService productPricingService,
			PriceLocalizationService priceLocalizationService) {
		this.promotionService = promotionService;
		this.productPricingService = productPricingService;
		this.priceLocalizationService = priceLocalizationService;
	}

	@PostMapping("/{productId}")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Promote owner product", description = "Debits the owner business wallet and promotes the selected product for the configured placement period.")
	public ProductPromotionResponse promote(
			@PathVariable Long productId,
			@Parameter(hidden = true) Principal principal) {
		return response(promotionService.promote(productId, principal.getName()), principal.getName());
	}

	@GetMapping("/owner")
	@Operation(summary = "List owner product promotions")
	public List<ProductPromotionResponse> ownerPromotions(@Parameter(hidden = true) Principal principal) {
		String actorUsername = principal.getName();
		return promotionService.ownerPromotions(actorUsername).stream()
				.map(promotion -> response(promotion, actorUsername))
				.toList();
	}

	@GetMapping("/active")
	@Operation(summary = "List active promoted products", description = "Returns the active promoted-product row shown above the normal user product catalogue.")
	public List<ProductPromotionResponse> activePromotions(
			@RequestParam(defaultValue = "12") int limit,
			@Parameter(hidden = true) Principal principal) {
		String actorUsername = principal.getName();
		return promotionService.activePromotions(limit).stream()
				.map(promotion -> response(promotion, actorUsername))
				.toList();
	}

	private ProductPromotionResponse response(ProductPromotion promotion, String actorUsername) {
		Product product = promotion.getProduct();
		BigDecimal salePrice = productPricingService.priceForSale(product);
		ProductResponse productResponse = ProductResponse.from(
				product,
				salePrice,
				priceLocalizationService.localize(product.getPrice(), actorUsername),
				priceLocalizationService.localize(salePrice, actorUsername),
				priceLocalizationService.localize(product.getReturnablePrice(), actorUsername),
				priceLocalizationService.localize(product.getNightShiftPrice(), actorUsername));
		return new ProductPromotionResponse(
				promotion.getId(),
				product.getId(),
				promotion.getBusiness().getId(),
				promotion.getBusiness().getName(),
				promotion.getPromotionPrice(),
				promotion.getBusinessAccountEntryId(),
				promotion.getStartsAt(),
				promotion.getEndsAt(),
				promotion.isCurrentlyActive(OffsetDateTime.now()),
				promotion.getCreatedAt(),
				productResponse);
	}
}
