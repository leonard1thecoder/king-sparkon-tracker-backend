package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.BarcodeImageDecodeResponse;
import com.king_sparkon_tracker.backend.dto.BarcodeVerificationResponse;
import com.king_sparkon_tracker.backend.service.BarcodeImageDecodeService;
import com.king_sparkon_tracker.backend.service.BarcodeVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/barcodes")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "AI barcode verify", description = "Hybrid camera/image barcode decoding, DB verification, and AI explanation endpoints.")
public class AiBarcodeVerificationController {

	private final BarcodeImageDecodeService barcodeImageDecodeService;
	private final BarcodeVerificationService barcodeVerificationService;

	public AiBarcodeVerificationController(
			BarcodeImageDecodeService barcodeImageDecodeService,
			BarcodeVerificationService barcodeVerificationService) {
		this.barcodeImageDecodeService = barcodeImageDecodeService;
		this.barcodeVerificationService = barcodeVerificationService;
	}

	@GetMapping("/verify/{value}")
	@Operation(summary = "Verify a scanned barcode or stock unit code", description = "Verifies browser-decoded values against productBarcode and unique unitCode records.")
	public BarcodeVerificationResponse verifyValue(
			@Parameter(description = "Browser-decoded product barcode or stock unit code.") @PathVariable String value,
			@Parameter(hidden = true) Principal principal) {
		return barcodeVerificationService.verify(value, principal.getName(), "BROWSER");
	}

	@PostMapping(value = "/images/decode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Decode a barcode image", description = "Uses ZXing to extract a barcode or stock unit code from an uploaded image.")
	public BarcodeImageDecodeResponse decodeImage(
			@Parameter(description = "Barcode image file.") @RequestPart("file") MultipartFile file) {
		return barcodeImageDecodeService.decode(file);
	}

	@PostMapping(value = "/images/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Decode and verify a barcode image", description = "Uses ZXing to decode the image, verifies the value in DB, and returns AI explanation.")
	public BarcodeVerificationResponse verifyImage(
			@Parameter(description = "Barcode image file.") @RequestPart("file") MultipartFile file,
			@Parameter(hidden = true) Principal principal) {
		BarcodeImageDecodeResponse decodeResponse = barcodeImageDecodeService.decode(file);
		return barcodeVerificationService.verify(decodeResponse.decodedValue(), principal.getName(), decodeResponse.decoder());
	}
}
