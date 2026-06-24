package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PromotionSchedulerService {

	private static final Logger log = LoggerFactory.getLogger(PromotionSchedulerService.class);

	private final PromotionService promotionService;

	public PromotionSchedulerService(PromotionService promotionService) {
		this.promotionService = promotionService;
	}

	@Scheduled(fixedDelayString = "${app.promotions.auto-create-delay-ms:3600000}", initialDelayString = "${app.promotions.auto-create-initial-delay-ms:60000}")
	public void createAutomatedKingSparkonPromotion() {
		try {
			promotionService.createAutomatedPromotionIfDue();
		} catch (RuntimeException exception) {
			log.warn("automated_promotion_create_failed reason={}", exception.getMessage());
		}
	}

	@Scheduled(fixedDelayString = "${app.promotions.process-delay-ms:3600000}", initialDelayString = "${app.promotions.process-initial-delay-ms:120000}")
	public void processDuePromotions() {
		try {
			promotionService.processDuePromotions();
		} catch (RuntimeException exception) {
			log.warn("promotion_processing_failed reason={}", exception.getMessage());
		}
	}
}
