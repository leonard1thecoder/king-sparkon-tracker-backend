package com.king_sparkon_tracker.backend.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.CreateTuckShopPurchaseRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.dto.ProductResponse;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseResponse;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.TuckShopService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tuck-shop")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "King Sparkon Tuck Shop", description = "Marketplace endpoints built on the existing product, barcode, transaction, Stripe, and worker-tip implementation.")
public class TuckShopController {

	private final TuckShopService tuckShopService;
	private final ProductPricingService productPricingService;
	private final PriceLocalizationService priceLocalizationService;

	public TuckShopController(
			TuckShopService tuckShopService,
			ProductPricingService productPricingService,
			PriceLocalizationService priceLocalizationService) {
		this.tuckShopService = tuckShopService;
		this.productPricingService = productPricingService;
		this.priceLocalizationService = priceLocalizationService;
	}

	@GetMapping("/products")
	public PageResponse<ProductResponse> products(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) Long businessId,
			@RequestParam(required = false) ProductCategory category,
			@RequestParam(required = false) String search,
			@Parameter(hidden = true) Principal principal) {
		String actorUsername = principal.getName();
		return PageResponse.from(
				tuckShopService.listAvailableProducts(pageable(page, size), businessId, category, search),
				product -> responseFrom(product, actorUsername)
		);
	}

	@GetMapping("/products/{productId}")
	public ProductResponse product(
			@PathVariable Long productId,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(tuckShopService.getAvailableProduct(productId), principal.getName());
	}

	@PostMapping("/purchases")
	@ResponseStatus(HttpStatus.CREATED)
	public TuckShopPurchaseResponse createSelfServicePurchase(
			@Valid @RequestBody CreateTuckShopPurchaseRequest request,
			@Parameter(hidden = true) Principal principal) {
		return tuckShopService.createSelfServicePurchase(request, principal.getName());
	}

	@PostMapping("/workers/barcode-purchases")
	@ResponseStatus(HttpStatus.CREATED)
	public TuckShopPurchaseResponse createWorkerBarcodePurchase(
			@Valid @RequestBody CreateTuckShopPurchaseRequest request,
			@Parameter(hidden = true) Principal principal) {
		return tuckShopService.createWorkerBarcodePurchase(request, principal.getName());
	}

	private ProductResponse responseFrom(Product product, String actorUsername) {
		BigDecimal salePrice = productPricingService.priceForSale(product);
		return ProductResponse.from(
				product,
				salePrice,
				priceLocalizationService.localize(product.getPrice(), actorUsername),
				priceLocalizationService.localize(salePrice, actorUsername),
				priceLocalizationService.localize(product.getReturnablePrice(), actorUsername),
				priceLocalizationService.localize(product.getNightShiftPrice(), actorUsername)
		);
	}

	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}
