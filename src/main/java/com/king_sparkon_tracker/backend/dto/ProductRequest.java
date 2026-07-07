package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@Schema(description = "Product display name.", example = "Coca-Cola 500ml")
		@NotBlank
		String name,

		@Schema(description = "Reusable retail product barcode/GTIN. This may be shared by many physical stock units.", example = "5449000000996")
		@JsonAlias({ "barcode", "gtin", "skuCode" })
		@Size(max = 128)
		String productBarcode,

		@Schema(description = "Business reporting category.", example = "Alcohol")
		@NotNull
		ProductCategory category,

		@Schema(description = "Current product unit price.", example = "20.50")
		@NotNull
		@DecimalMin(value = "0.00")
		BigDecimal price,

		@JsonAlias("bottleReturnable")
		@Schema(description = "Whether this product charges a returnable packaging/deposit fee.", example = "true")
		@NotNull
		Boolean returnableEnabled,

		@Schema(description = "Owner configured returnable charge. Used only when returnableEnabled is true.", example = "5.00")
		@DecimalMin(value = "0.00")
		BigDecimal returnablePrice,

		@Schema(description = "Whether this product has nightshift pricing enabled.", example = "true")
		@NotNull
		Boolean nightShiftEnabled,

		@Schema(description = "Owner configured nightshift surcharge. Used only when nightShiftEnabled is true.", example = "7.50")
		@DecimalMin(value = "0.00")
		BigDecimal nightShiftPrice,

		@Schema(description = "Nightshift start time configured by owner.", example = "22:00:00")
		LocalTime nightShiftStartTime,

		@Schema(description = "Nightshift end time configured by owner.", example = "06:00:00")
		LocalTime nightShiftEndTime,

		@Schema(description = "Opening stock quantity.", example = "10")
		@NotNull
		@PositiveOrZero
		Integer stockQuantity,

		@Schema(description = "Product photo URL/path shown in King Sparkon Tuck Shop.", example = "https://storage.googleapis.com/king-sparkon/products/coke.png")
		@Size(max = 2048)
		String productImageUrl
) {
	public ProductRequest(
			String name,
			ProductCategory category,
			BigDecimal price,
			Boolean returnableEnabled,
			BigDecimal returnablePrice,
			Boolean nightShiftEnabled,
			BigDecimal nightShiftPrice,
			LocalTime nightShiftStartTime,
			LocalTime nightShiftEndTime,
			Integer stockQuantity,
			String productImageUrl) {
		this(
				name,
				null,
				category,
				price,
				returnableEnabled,
				returnablePrice,
				nightShiftEnabled,
				nightShiftPrice,
				nightShiftStartTime,
				nightShiftEndTime,
				stockQuantity,
				productImageUrl
		);
	}

	public ProductRequest(
			String name,
			ProductCategory category,
			BigDecimal price,
			Boolean returnableEnabled,
			BigDecimal returnablePrice,
			Boolean nightShiftEnabled,
			BigDecimal nightShiftPrice,
			LocalTime nightShiftStartTime,
			LocalTime nightShiftEndTime,
			Integer stockQuantity) {
		this(
				name,
				category,
				price,
				returnableEnabled,
				returnablePrice,
				nightShiftEnabled,
				nightShiftPrice,
				nightShiftStartTime,
				nightShiftEndTime,
				stockQuantity,
				null
		);
	}
}
