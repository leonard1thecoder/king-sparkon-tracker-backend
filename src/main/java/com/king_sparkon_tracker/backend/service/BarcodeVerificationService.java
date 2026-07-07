package com.king_sparkon_tracker.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.BarcodeMatchType;
import com.king_sparkon_tracker.backend.dto.BarcodeVerificationResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class BarcodeVerificationService {

	private final ProductRepository productRepository;
	private final ProductBarcodeRepository productBarcodeRepository;
	private final TrackerUserService userService;
	private final AiBarcodeExplanationService aiBarcodeExplanationService;

	public BarcodeVerificationService(
			ProductRepository productRepository,
			ProductBarcodeRepository productBarcodeRepository,
			TrackerUserService userService,
			AiBarcodeExplanationService aiBarcodeExplanationService) {
		this.productRepository = productRepository;
		this.productBarcodeRepository = productBarcodeRepository;
		this.userService = userService;
		this.aiBarcodeExplanationService = aiBarcodeExplanationService;
	}

	public BarcodeVerificationResponse verify(String value, String actorUsername, String decoder) {
		String normalizedValue = normalizeRequired(value, "Barcode value is required");
		Business business = userService.businessForActor(actorUsername);

		BarcodeVerificationResponse response = productBarcodeRepository.findByUnitCode(normalizedValue, business.getId())
				.map(unit -> stockUnitResponse(normalizedValue, decoder, unit))
				.or(() -> productRepository.findFirstByProductBarcodeAndBusiness_Id(normalizedValue, business.getId())
						.map(product -> productBarcodeResponse(normalizedValue, decoder, product)))
				.orElseGet(() -> notFoundResponse(normalizedValue, decoder));

		return withAiExplanation(response);
	}

	private BarcodeVerificationResponse stockUnitResponse(String input, String decoder, ProductBarcode unit) {
		Product product = unit.getProduct();
		return new BarcodeVerificationResponse(
				input,
				input,
				decoder,
				BarcodeMatchType.STOCK_UNIT,
				"FOUND",
				product.getId(),
				product.getName(),
				product.getProductBarcode(),
				product.getStockQuantity(),
				unit.getUnitCode(),
				unit.getAvailabilityStatus(),
				unit.getStatus(),
				"Unique stock unit found for product: " + product.getName(),
				null
		);
	}

	private BarcodeVerificationResponse productBarcodeResponse(String input, String decoder, Product product) {
		return new BarcodeVerificationResponse(
				input,
				input,
				decoder,
				BarcodeMatchType.PRODUCT_BARCODE,
				"FOUND",
				product.getId(),
				product.getName(),
				product.getProductBarcode(),
				product.getStockQuantity(),
				null,
				null,
				null,
				"Reusable product barcode found for product: " + product.getName(),
				null
		);
	}

	private BarcodeVerificationResponse notFoundResponse(String input, String decoder) {
		return new BarcodeVerificationResponse(
				input,
				input,
				decoder,
				BarcodeMatchType.NOT_FOUND,
				"NOT_FOUND",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"No matching product barcode or stock unit code was found",
				null
		);
	}

	private BarcodeVerificationResponse withAiExplanation(BarcodeVerificationResponse response) {
		String explanation = aiBarcodeExplanationService.explain(response);
		return new BarcodeVerificationResponse(
				response.input(),
				response.normalizedValue(),
				response.decoder(),
				response.matchType(),
				response.status(),
				response.productId(),
				response.productName(),
				response.productBarcode(),
				response.stockQuantity(),
				response.unitCode(),
				response.availabilityStatus(),
				response.claimStatus(),
				response.message(),
				explanation
		);
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}
}
