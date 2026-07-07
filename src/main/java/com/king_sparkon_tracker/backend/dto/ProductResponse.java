package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductResponse(
		@Schema(description = "Product id.", example = "9")
		Long id,

		@Schema(description = "Business id that owns this product.", example = "3")
		Long businessId,

		@Schema(description = "Business name that owns this product.", example = "Owner Retail Store")
		String businessName,

		@Schema(description = "Product name.", example = "Barcode item")
		String name,

		@Schema(description = "Reusable retail product barcode/GTIN. This is not globally unique across stock units.", example = "5449000000996")
		String productBarcode,

		@Schema(description = "Linked global barcode catalog id, when available.", example = "12")
		Long barcodeCatalogId,

		@Schema(description = "Product photo URL/path shown in King Sparkon Tuck Shop.")
		String productImageUrl,

		@Schema(description = "Product category.", example = "Alcohol")
		ProductCategory category,

		@Schema(description = "Product lifecycle status.", example = "CREATED")
		ProductStatus status,

		@Schema(description = "Product unit price in base currency.", example = "20.50")
		BigDecimal price,

		@Schema(description = "Current sale price in base currency after active owner-configured charges.", example = "33.00")
		BigDecimal salePrice,

		@Schema(description = "Base currency stored by the backend.", example = "ZAR")
		SupportedCurrency baseCurrency,

		@Schema(description = "Localized product unit price for the user.")
		MoneyResponse localizedPrice,

		@Schema(description = "Localized sale price for the user.")
		MoneyResponse localizedSalePrice,

		@Schema(description = "Whether returnable pricing is enabled.", example = "true")
		boolean returnableEnabled,

		@Schema(description = "Owner configured returnable charge in base currency.", example = "5.00")
		BigDecimal returnablePrice,

		@Schema(description = "Localized returnable charge for the user.")
		MoneyResponse localizedReturnablePrice,

		@Schema(description = "Whether nightshift pricing is enabled.", example = "true")
		boolean nightShiftEnabled,

		@Schema(description = "Owner configured nightshift surcharge in base currency.", example = "7.50")
		BigDecimal nightShiftPrice,

		@Schema(description = "Localized nightshift surcharge for the user.")
		MoneyResponse localizedNightShiftPrice,

		@Schema(description = "Nightshift start time.", example = "22:00:00")
		LocalTime nightShiftStartTime,

		@Schema(description = "Nightshift end time.", example = "06:00:00")
		LocalTime nightShiftEndTime,

		@Schema(description = "Backward-compatible alias. Use returnableEnabled in new clients.", example = "true")
		boolean bottleReturnable,

		@Schema(description = "Current stock quantity.", example = "10")
		int stockQuantity,

		@Schema(description = "Assigned physical stock units for this product, including unit code and claim status.")
		List<ProductBarcodeResponse> barcodes,

		@Schema(description = "Number of stock units assigned to this product.", example = "8")
		int barcodeCount,

		@Schema(description = "How many more stock units can be assigned before reaching stock quantity.", example = "2")
		int remainingBarcodeSlots
) {

	public ProductResponse(
			Long id,
			Long businessId,
			String businessName,
			String name,
			ProductCategory category,
			ProductStatus status,
			BigDecimal price,
			BigDecimal salePrice,
			SupportedCurrency baseCurrency,
			MoneyResponse localizedPrice,
			MoneyResponse localizedSalePrice,
			boolean returnableEnabled,
			BigDecimal returnablePrice,
			MoneyResponse localizedReturnablePrice,
			boolean nightShiftEnabled,
			BigDecimal nightShiftPrice,
			MoneyResponse localizedNightShiftPrice,
			LocalTime nightShiftStartTime,
			LocalTime nightShiftEndTime,
			boolean bottleReturnable,
			int stockQuantity,
			List<ProductBarcodeResponse> barcodes,
			int barcodeCount,
			int remainingBarcodeSlots) {
		this(
				id,
				businessId,
				businessName,
				name,
				null,
				null,
				null,
				category,
				status,
				price,
				salePrice,
				baseCurrency,
				localizedPrice,
				localizedSalePrice,
				returnableEnabled,
				returnablePrice,
				localizedReturnablePrice,
				nightShiftEnabled,
				nightShiftPrice,
				localizedNightShiftPrice,
				nightShiftStartTime,
				nightShiftEndTime,
				bottleReturnable,
				stockQuantity,
				barcodes,
				barcodeCount,
				remainingBarcodeSlots);
	}

	public static ProductResponse from(
			Product product,
			BigDecimal salePrice,
			MoneyResponse localizedPrice,
			MoneyResponse localizedSalePrice,
			MoneyResponse localizedReturnablePrice,
			MoneyResponse localizedNightShiftPrice) {
		List<ProductBarcodeResponse> barcodes = product.getBarcodes().stream()
				.map(ProductBarcodeResponse::from)
				.toList();

		return new ProductResponse(
				product.getId(),
				product.getBusiness() == null ? null : product.getBusiness().getId(),
				product.getBusiness() == null ? null : product.getBusiness().getName(),
				product.getName(),
				product.getProductBarcode(),
				product.getBarcodeCatalog() == null ? null : product.getBarcodeCatalog().getId(),
				product.getProductImageUrl(),
				product.getCategory(),
				product.getStatus(),
				product.getPrice(),
				salePrice,
				SupportedCurrency.ZAR,
				localizedPrice,
				localizedSalePrice,
				product.isReturnableEnabled(),
				product.getReturnablePrice(),
				localizedReturnablePrice,
				product.isNightShiftEnabled(),
				product.getNightShiftPrice(),
				localizedNightShiftPrice,
				product.getNightShiftStartTime(),
				product.getNightShiftEndTime(),
				product.isReturnableEnabled(),
				product.getStockQuantity(),
				barcodes,
				barcodes.size(),
				Math.max(product.getStockQuantity() - barcodes.size(), 0)
		);
	}
}
