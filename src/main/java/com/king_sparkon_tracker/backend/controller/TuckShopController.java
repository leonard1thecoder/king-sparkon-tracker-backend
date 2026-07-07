package com.king_sparkon_tracker.backend.controller;

import java.math.BigDecimal;
import java.security.Principal;

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
import com.king_sparkon_tracker.backend.service.ProductPageableFactory;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.TuckShopService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tuck-shop")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "King Sparkon Tuck Shop")
public class TuckShopController {

	private final TuckShopService tuckShopService;
	private final ProductPricingService productPricingService;
	private final PriceLocalizationService priceLocalizationService;
	private final ProductPageableFactory productPageableFactory;

	public TuckShopController(
			TuckShopService tuckShopService,
			ProductPricingService productPricingService,
			PriceLocalizationService priceLocalizationService,
			ProductPageableFactory productPageableFactory) {
		this.tuckShopService = tuckShopService;
		this.productPricingService = productPricingService;
		this.priceLocalizationService = priceLocalizationService;
		this.productPageableFactory = productPageableFactory;
	}

	@GetMapping("/products")
	@Operation(summary = "Search, filter, sort, and page tuck shop products")
	public PageResponse<ProductResponse> products(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "name") String sortBy,
			@RequestParam(defaultValue = "asc") String direction,
			@RequestParam(required = false) Long businessId,
			@RequestParam(required = false) ProductCategory category,
			@RequestParam(required = false, name = "q") String query,
			@RequestParam(required = false) String search,
			@Parameter(hidden = true) Principal principal) {
		String actorUsername = principal.getName();
		String effectiveSearch = query == null || query.isBlank() ? search : query;
		Pageable pageable = productPageableFactory.create(page, size, sortBy, direction);
		return PageResponse.from(
				tuckShopService.listAvailableProducts(pageable, businessId, category, effectiveSearch),
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
}
