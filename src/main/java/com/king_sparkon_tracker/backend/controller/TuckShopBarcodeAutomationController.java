package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.CreateTuckShopPurchaseRequest;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseResponse;
import com.king_sparkon_tracker.backend.service.ProductBarcodeAutomationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tuck-shop/workers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Worker automatic barcode checkout")
public class TuckShopBarcodeAutomationController {

	private final ProductBarcodeAutomationService service;

	public TuckShopBarcodeAutomationController(ProductBarcodeAutomationService service) {
		this.service = service;
	}

	@PostMapping("/automatic-purchases")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a worker checkout that automatically fills generated stock-unit codes")
	public TuckShopPurchaseResponse createPurchase(
			@Valid @RequestBody CreateTuckShopPurchaseRequest request,
			@Parameter(hidden = true) Principal principal) {
		return service.createWorkerPurchase(request, principal.getName());
	}

	@PostMapping("/online-purchases/{transactionId}/products/{productId}/auto-barcodes")
	@Operation(summary = "Assign all remaining generated barcodes to one paid online product line")
	public TuckShopPurchaseResponse prepareAutomaticLine(
			@PathVariable Long transactionId,
			@PathVariable Long productId,
			@Parameter(hidden = true) Principal principal) {
		return service.prepareAutomaticOnlineLine(transactionId, productId, principal.getName());
	}

	@GetMapping("/completed-purchases")
	@Operation(summary = "List paid and collected product carts for the worker business")
	public List<TuckShopPurchaseResponse> completedPurchases(@Parameter(hidden = true) Principal principal) {
		return service.listCompletedWorkerSales(principal.getName());
	}
}
