package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CampaignPricingServiceTest {

	private final PromotionPricingService pricingService = new PromotionPricingService();

	@Test
	void quoteUsesSmallAudienceTier() {
		var quote = pricingService.quote(100);

		assertThat(quote.platformFee()).isEqualByComparingTo("49.00");
		assertThat(quote.pricePerSubscriber()).isEqualByComparingTo("0.90");
		assertThat(quote.totalPrice()).isEqualByComparingTo("139.00");
		assertThat(quote.tier()).isEqualTo("1-100");
	}

	@Test
	void quoteUsesMediumAudienceTier() {
		var quote = pricingService.quote(250);

		assertThat(quote.pricePerSubscriber()).isEqualByComparingTo("0.65");
		assertThat(quote.totalPrice()).isEqualByComparingTo("211.50");
		assertThat(quote.tier()).isEqualTo("101-1000");
	}

	@Test
	void quoteUsesLargeAudienceTier() {
		var quote = pricingService.quote(1500);

		assertThat(quote.pricePerSubscriber()).isEqualByComparingTo("0.45");
		assertThat(quote.totalPrice()).isEqualByComparingTo("724.00");
		assertThat(quote.tier()).isEqualTo("1001+");
	}
}
