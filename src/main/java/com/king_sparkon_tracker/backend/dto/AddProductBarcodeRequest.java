package com.king_sparkon_tracker.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddProductBarcodeRequest(
		@Schema(description = "Unique barcode for one physical stocked item.", example = "6001")
		@NotBlank
		String barcode,
		@Schema(description = "Optional customer email reference for returnable claims.", example = "customer@example.com")
		@JsonAlias("referencee")
		@Email
		@Size(max = 255)
		String referenceEmail) {
}
