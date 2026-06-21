package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.ProductBarcodeLookupResponse;
import com.king_sparkon_tracker.backend.service.ProductBarcodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/barcodes")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Product barcodes", description = "Reference search and returnable barcode claim endpoints.")
public class ProductBarcodeController {

	private final ProductBarcodeService productBarcodeService;

	public ProductBarcodeController(ProductBarcodeService productBarcodeService) {
		this.productBarcodeService = productBarcodeService;
	}

	/**
	 * Finds barcode claim rows by reference so frontend tables can show claimable returnable records.
	 */
	@GetMapping("/reference/{referencee}")
	@Operation(summary = "Find barcodes by reference", description = "Returns barcode rows matching a customer reference.")
	public List<ProductBarcodeLookupResponse> findByReference(
			@Parameter(description = "Customer reference, usually a cellphone number.") @PathVariable String referencee,
			@Parameter(hidden = true) Principal principal) {
		return productBarcodeService.findByReference(referencee, principal.getName()).stream()
				.map(ProductBarcodeLookupResponse::from)
				.toList();
	}

	/**
	 * Claims a single active returnable barcode when the supplied reference maps to one unambiguous claim.
	 */
	@PostMapping("/reference/{referencee}/claim")
	@Operation(summary = "Claim barcode by reference", description = "Marks one active returnable barcode as claimed by reference.")
	public ProductBarcodeLookupResponse claimByReference(
			@Parameter(description = "Customer reference, usually a cellphone number.") @PathVariable String referencee,
			@Parameter(hidden = true) Principal principal) {
		return ProductBarcodeLookupResponse.from(productBarcodeService.claimByReference(referencee, principal.getName()));
	}

	/**
	 * Claims a selected barcode row after a reference search returns multiple rows.
	 */
	@PostMapping("/{id}/claim")
	@Operation(summary = "Claim barcode by id", description = "Marks the selected active returnable barcode as claimed.")
	public ProductBarcodeLookupResponse claimById(
			@Parameter(description = "Barcode id.") @PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return ProductBarcodeLookupResponse.from(productBarcodeService.claimById(id, principal.getName()));
	}
}
