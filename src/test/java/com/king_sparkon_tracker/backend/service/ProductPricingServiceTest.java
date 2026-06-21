package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TransactionType;

class ProductPricingServiceTest {

	@Test
	void priceForSaleAddsConfiguredReturnableAndNightShiftCharges() {
		ProductPricingService service = pricingServiceAt(LocalDateTime.of(2026, 6, 1, 8, 0));

		assertThat(service.priceForSale(returnableNightShiftProduct())).isEqualByComparingTo("26.00");
	}

	@Test
	void priceForSaleKeepsReturnableChargeOutsideNightShift() {
		ProductPricingService service = pricingServiceAt(LocalDateTime.of(2026, 6, 1, 12, 0));

		assertThat(service.priceForSale(returnableNightShiftProduct())).isEqualByComparingTo("23.00");
	}

	@Test
	void priceForSaleSkipsNightShiftWhenWindowIsNotConfigured() {
		ProductPricingService service = pricingServiceAt(LocalDateTime.of(2026, 6, 1, 8, 0));

		assertThat(service.priceForSale(returnableProduct())).isEqualByComparingTo("23.00");
	}

	@Test
	void priceForSaleSupportsOvernightNightShiftWindows() {
		ProductPricingService service = pricingServiceAt(LocalDateTime.of(2026, 6, 1, 23, 0));

		assertThat(service.priceForSale(overnightNightShiftProduct())).isEqualByComparingTo("30.50");
	}

	@Test
	void priceForTransactionDoesNotAddSurchargesToBuyMovements() {
		ProductPricingService service = pricingServiceAt(LocalDateTime.of(2026, 6, 1, 8, 0));

		assertThat(service.priceForTransaction(returnableNightShiftProduct(), TransactionType.BUY, null))
				.isEqualByComparingTo("20.00");
	}

	private Product returnableProduct() {
		return new Product(
				"Beer",
				ProductCategory.Alcohol,
				new BigDecimal("20.00"),
				10,
				true,
				new BigDecimal("3.00"),
				false,
				BigDecimal.ZERO,
				null,
				null,
				null);
	}

	private Product returnableNightShiftProduct() {
		return new Product(
				"Beer",
				ProductCategory.Alcohol,
				new BigDecimal("20.00"),
				10,
				true,
				new BigDecimal("3.00"),
				true,
				new BigDecimal("3.00"),
				LocalTime.of(0, 0),
				LocalTime.of(9, 30),
				null);
	}

	private Product overnightNightShiftProduct() {
		return new Product(
				"Beer",
				ProductCategory.Alcohol,
				new BigDecimal("20.00"),
				10,
				true,
				new BigDecimal("3.00"),
				true,
				new BigDecimal("7.50"),
				LocalTime.of(22, 0),
				LocalTime.of(6, 0),
				null);
	}

	private ProductPricingService pricingServiceAt(LocalDateTime dateTime) {
		ZoneId zone = ZoneId.of("Africa/Johannesburg");
		Clock fixedClock = Clock.fixed(dateTime.atZone(zone).toInstant(), zone);
		return new ProductPricingService(fixedClock);
	}
}
