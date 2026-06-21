package com.king_sparkon_tracker.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BillingStatusScheduler {

	private final BusinessBillingService businessBillingService;

	public BillingStatusScheduler(BusinessBillingService businessBillingService) {
		this.businessBillingService = businessBillingService;
	}

	@Scheduled(cron = "0 15 * * * *", zone = "Africa/Johannesburg")
	public void deactivateExpiredTrialsAndUnpaidBusinesses() {
		businessBillingService.deactivateExpiredTrialsAndUnpaidBusinesses();
	}
}
