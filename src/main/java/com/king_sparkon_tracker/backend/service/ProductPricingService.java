package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.TransactionType;

@Service
public class ProductPricingService {

	private final Clock clock;

	public ProductPricingService(Clock clock) {
		this.clock = clock;
	}

	public BigDecimal priceForSale(Product product) {
		return priceForTransaction(product, TransactionType.SELL, null);
	}

	public BigDecimal priceForTransaction(Product product, TransactionType type, BigDecimal overrideUnitPrice) {
		BigDecimal unitPrice = overrideUnitPrice == null ? product.getPrice() : overrideUnitPrice;

		if (type != TransactionType.SELL) {
			return unitPrice;
		}

		if (product.isReturnableEnabled()) {
			unitPrice = unitPrice.add(safeMoney(product.getReturnablePrice()));
		}

		if (product.isNightShiftEnabled() && isInsideNightShiftWindow(product, LocalDateTime.now(clock).toLocalTime())) {
			unitPrice = unitPrice.add(safeMoney(product.getNightShiftPrice()));
		}

		return unitPrice;
	}

	private boolean isInsideNightShiftWindow(Product product, LocalTime currentTime) {
		LocalTime start = product.getNightShiftStartTime();
		LocalTime end = product.getNightShiftEndTime();

		if (start == null || end == null) {
			return false;
		}

		if (start.equals(end)) {
			return false;
		}

		if (start.isBefore(end)) {
			return !currentTime.isBefore(start) && currentTime.isBefore(end);
		}

		return !currentTime.isBefore(start) || currentTime.isBefore(end);
	}

	private BigDecimal safeMoney(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
