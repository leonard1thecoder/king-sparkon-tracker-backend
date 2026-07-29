package com.king_sparkon_tracker.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional(readOnly = true)
public class BusinessAccessService {

	private final TrackerUserRepository userRepository;

	public BusinessAccessService(
			TrackerUserRepository userRepository,
			BusinessPlanPolicyService businessPlanPolicyService) {
		this.userRepository = userRepository;
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
		businessForActor(actorUsername);
	}

	public void requireFeature(String actorUsername, BusinessFeature feature) {
		businessForActor(actorUsername);
	}
}
