package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.ProductBarcodeModeRequest;
import com.king_sparkon_tracker.backend.dto.ProductBarcodeModeResponse;
import com.king_sparkon_tracker.backend.service.ProductBarcodeAutomationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products/barcode-automation")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Product barcode automation")
public class ProductBarcodeAutomationController {

	private final ProductBarcodeAutomationService service;

	public ProductBarcodeAutomationController(ProductBarcodeAutomationService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List barcode mode and remaining barcode requirement for business products")
	public List<ProductBarcodeModeResponse> list(@Parameter(hidden = true) Principal principal) {
		return service.listConfigurations(principal.getName());
	}

	@PutMapping("/{productId}")
	public ProductBarcodeModeResponse configure(
			@PathVariable Long productId,
			@Valid @RequestBody ProductBarcodeModeRequest request,
			@Parameter(hidden = true) Principal principal) {
		return service.configure(productId, request, principal.getName());
	}

	@PostMapping("/{productId}/fill-stock")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create every missing internal stock-unit barcode for a non-barcoded product")
	public ProductBarcodeModeResponse fillStock(
			@PathVariable Long productId,
			@Parameter(hidden = true) Principal principal) {
		return service.fillAutomaticStock(productId, principal.getName());
	}
}
