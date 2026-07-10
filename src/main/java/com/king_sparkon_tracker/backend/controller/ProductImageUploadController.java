package com.king_sparkon_tracker.backend.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.ProductResponse;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.service.GoogleStorageService;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProductImageUploadController {

	private final ProductService productService;
	private final GoogleStorageService googleStorageService;
	private final ProductPricingService productPricingService;
	private final PriceLocalizationService priceLocalizationService;

	public ProductImageUploadController(
			ProductService productService,
			GoogleStorageService googleStorageService,
			ProductPricingService productPricingService,
			PriceLocalizationService priceLocalizationService) {
		this.productService = productService;
		this.googleStorageService = googleStorageService;
		this.productPricingService = productPricingService;
		this.priceLocalizationService = priceLocalizationService;
	}

	@PatchMapping(path = "/{id}/image-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a local product image to Google Storage")
	public ProductResponse uploadProductImage(
			@PathVariable Long id,
			@RequestPart("file") MultipartFile file,
			Principal principal) {
		String actorUsername = principal.getName();
		Product existing = productService.getProductById(id, actorUsername);
		GoogleStorageService.StoredImage storedImage = googleStorageService.storeImage(
				file,
				"products",
				"business-%s-product-%s".formatted(existing.getBusiness().getId(), existing.getId()));
		Product updated = productService.updateProductImageUrl(id, storedImage.url(), actorUsername);
		return responseFrom(updated, actorUsername);
	}

	private ProductResponse responseFrom(Product product, String actorUsername) {
		BigDecimal salePrice = productPricingService.priceForSale(product);
		return ProductResponse.from(
				product,
				salePrice,
				priceLocalizationService.localize(product.getPrice(), actorUsername),
				priceLocalizationService.localize(salePrice, actorUsername),
				priceLocalizationService.localize(product.getReturnablePrice(), actorUsername),
				priceLocalizationService.localize(product.getNightShiftPrice(), actorUsername));
	}
}
