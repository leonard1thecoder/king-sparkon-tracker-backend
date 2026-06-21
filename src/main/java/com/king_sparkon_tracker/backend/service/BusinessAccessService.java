package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional(readOnly = true)
public class BusinessAccessService {

	private static final Logger log = LoggerFactory.getLogger(BusinessAccessService.class);

	private final TrackerUserRepository userRepository;
	private final BusinessPlanPolicyService businessPlanPolicyService;

	public BusinessAccessService(
			TrackerUserRepository userRepository,
			BusinessPlanPolicyService businessPlanPolicyService) {
		this.userRepository = userRepository;
		this.businessPlanPolicyService = businessPlanPolicyService;
	}

	public Business businessForActor(String actorUsername) {
		TrackerUser user = userRepository.findByUsername(actorUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));

		if (user.getBusiness() == null) {
			throw new IllegalArgumentException("User is not linked to a business");
		}

		return user.getBusiness();
	}

	public void requireActiveBusiness(String actorUsername) {
		Business business = businessForActor(actorUsername);

		if (!businessPlanPolicyService.isActiveOrTrial(business)) {
			log.warn(
					"business_access_rejected businessId={} status={} actor={}",
					business.getId(),
					business.getBusinessStatus(),
					actorUsername
			);

			if (business.getBusinessStatus() == BusinessStatus.DEACTIVATED) {
				throw new IllegalArgumentException("Business is deactivated. Please activate your subscription");
			}

			throw new IllegalArgumentException("Business subscription is not active");
		}
	}

	public void requireFeature(String actorUsername, BusinessFeature feature) {
		Business business = businessForActor(actorUsername);

		if (!businessPlanPolicyService.isFeatureEnabled(business, feature)) {
			log.warn(
					"business_feature_rejected businessId={} status={} plan={} feature={} actor={}",
					business.getId(),
					business.getBusinessStatus(),
					business.getBusinessPlan(),
					feature,
					actorUsername
			);

			if (business.getBusinessStatus() == BusinessStatus.DEACTIVATED) {
				throw new IllegalArgumentException("Business is deactivated. Please activate your subscription");
			}

			throw new IllegalArgumentException("Feature " + feature + " is not available for this business plan");
		}
	}
}
