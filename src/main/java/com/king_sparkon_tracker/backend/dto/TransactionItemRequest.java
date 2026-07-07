package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record TransactionItemRequest(
		@Schema(description = "Product id to move.", example = "9") @NotNull Long productId,
		@Schema(description = "Positive quantity to buy or sell.", example = "2") @NotNull @Positive Integer quantity,
		@Schema(description = "Optional unit price override. Product price is used when omitted.", example = "20.50") @DecimalMin(value = "0.00") BigDecimal unitPrice,
		@Schema(description = "Scanned unique stock unit codes for SELL movements. Count must match quantity when provided.")
		List<String> barcodes,
		@Schema(description = "Customer email reference for returnable stock unit claims.", example = "customer@example.com")
		@Email
		@Size(max = 255)
		String referenceEmail) {

	public TransactionItemRequest(Long productId, Integer quantity, BigDecimal unitPrice) {
		this(productId, quantity, unitPrice, List.of(), null);
	}

	public TransactionItemRequest(Long productId, Integer quantity, BigDecimal unitPrice, List<String> barcodes) {
		this(productId, quantity, unitPrice, barcodes, null);
	}
}
