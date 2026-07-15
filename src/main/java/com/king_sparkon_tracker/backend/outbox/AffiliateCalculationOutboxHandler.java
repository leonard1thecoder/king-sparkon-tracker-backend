package com.king_sparkon_tracker.backend.outbox;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.BusinessSubscriptionRepository;
import com.king_sparkon_tracker.backend.service.AffiliateService;

@Component
public class AffiliateCalculationOutboxHandler implements OutboxEventHandler {
	private final ObjectMapper objectMapper;
	private final BusinessSubscriptionRepository subscriptionRepository;
	private final BusinessRepository businessRepository;
	private final AffiliateService affiliateService;

	public AffiliateCalculationOutboxHandler(
			ObjectMapper objectMapper,
			BusinessSubscriptionRepository subscriptionRepository,
			BusinessRepository businessRepository,
			AffiliateService affiliateService) {
		this.objectMapper = objectMapper;
		this.subscriptionRepository = subscriptionRepository;
		this.businessRepository = businessRepository;
		this.affiliateService = affiliateService;
	}
	@Override public OutboxEventType supports() { return OutboxEventType.AFFILIATE_COMMISSION_CALCULATION; }
	@Override public void handle(OutboxEvent event) throws Exception {
		OutboxPayloads.AffiliateCalculation payload = objectMapper.readValue(event.getPayload(), OutboxPayloads.AffiliateCalculation.class);
		BusinessSubscription subscription = subscriptionRepository.findById(payload.businessSubscriptionId())
				.orElseThrow(() -> new IllegalArgumentException("Business subscription not found for affiliate calculation"));
		Business business = businessRepository.findById(payload.businessId())
				.orElseThrow(() -> new IllegalArgumentException("Business not found for affiliate calculation"));
		affiliateService.recordCommission(subscription, business, payload.earnedAt());
	}
}
