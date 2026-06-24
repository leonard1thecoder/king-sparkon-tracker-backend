package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.config.RedisCacheConfig;
import com.king_sparkon_tracker.backend.dto.PromotionPriceQuoteResponse;

@Service
public class PromotionPricingService {

	private static final BigDecimal PLATFORM_FEE = new BigDecimal("49.00");
	private static final int SCALE = 2;

	@Cacheable(cacheNames = RedisCacheConfig.PROMOTION_QUOTES_CACHE, key = "#targetCount")
	public PromotionPriceQuoteResponse quote(int targetCount) {
		int safeTargetCount = Math.max(targetCount, 0);
		Tier tier = tierFor(safeTargetCount);
		BigDecimal total = PLATFORM_FEE.add(tier.pricePerSubscriber.multiply(BigDecimal.valueOf(safeTargetCount)))
				.setScale(SCALE, RoundingMode.HALF_UP);
		return new PromotionPriceQuoteResponse(
				safeTargetCount,
				PLATFORM_FEE,
				tier.pricePerSubscriber.setScale(SCALE, RoundingMode.HALF_UP),
				total,
				tier.label);
	}

	private Tier tierFor(int targetCount) {
		if (targetCount <= 100) {
			return new Tier("1-100", new BigDecimal("0.90"));
		}
		if (targetCount <= 1000) {
			return new Tier("101-1000", new BigDecimal("0.65"));
		}
		return new Tier("1001+", new BigDecimal("0.45"));
	}

	private record Tier(String label, BigDecimal pricePerSubscriber) {
	}
}
