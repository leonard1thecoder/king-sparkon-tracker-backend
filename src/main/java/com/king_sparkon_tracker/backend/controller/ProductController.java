package com.king_sparkon_tracker.backend.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.ProductResponse;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Products", description = "Barcode-based product catalogue and stock lookup endpoints.")
public class ProductController {

	private final ProductService productService;
	private final ProductPricingService productPricingService;
	private final PriceLocalizationService priceLocalizationService;

	public ProductController(
			ProductService productService,
			ProductPricingService productPricingService,
			PriceLocalizationService priceLocalizationService) {
		this.productService = productService;
		this.productPricingService = productPricingService;
		this.priceLocalizationService = priceLocalizationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse createProduct(
			@Valid @RequestBody ProductRequest request,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(productService.createProduct(request, principal.getName()), principal.getName());
	}

	@PostMapping("/{id}/barcodes")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse addBarcodeToProduct(
			@PathVariable Long id,
			@Valid @RequestBody AddProductBarcodeRequest request,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(productService.addBarcodeToProduct(id, request, principal.getName()), principal.getName());
	}

	@PatchMapping("/{id}/quantity")
	public ProductResponse updateProductQuantity(
			@PathVariable Long id,
			@Valid @RequestBody UpdateProductQuantityRequest request,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(productService.updateProductQuantity(id, request, principal.getName()), principal.getName());
	}

	@PostMapping("/{id}/submit-approval")
	public ProductResponse submitProductForApproval(
			@PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(productService.submitProductForApproval(id, principal.getName()), principal.getName());
	}

	@GetMapping
	public PageResponse<ProductResponse> listProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) Principal principal) {
		String actorUsername = principal.getName();

		return PageResponse.from(
				productService.listProducts(pageable(page, size), actorUsername),
				product -> responseFrom(product, actorUsername)
		);
	}

	@GetMapping("/{id}")
	public ProductResponse getProductById(
			@PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(productService.getProductById(id, principal.getName()), principal.getName());
	}

	@GetMapping("/barcode/{barcode}")
	public ProductResponse getProductByBarcode(
			@PathVariable String barcode,
			@Parameter(hidden = true) Principal principal) {
		return responseFrom(productService.getProductByBarcode(barcode, principal.getName()), principal.getName());
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
