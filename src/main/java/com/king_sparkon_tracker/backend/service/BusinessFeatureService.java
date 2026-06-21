package com.king_sparkon_tracker.backend.service;

import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.BusinessFeature;

@Service
public class BusinessFeatureService {

	private final BusinessAccessService businessAccessService;

	public BusinessFeatureService(BusinessAccessService businessAccessService) {
		this.businessAccessService = businessAccessService;
	}

	public void requireWorkerTipsPlatform(String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.WORKER_TIPS_PLATFORM);
	}

	public void requireBusinessAnalysisAi(String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.BUSINESS_ANALYSIS_AI);
	}

	public void requireWorkerClocker(String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.WORKER_CLOCKER);
	}
}
