package com.king_sparkon_tracker.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.AffiliateLeadResponse;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.SubscriberRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional(readOnly = true)
public class AffiliateLeadService {

	private final SubscriberRepository subscriberRepository;
	private final TrackerUserRepository userRepository;

	public AffiliateLeadService(
			SubscriberRepository subscriberRepository,
			TrackerUserRepository userRepository) {
		this.subscriberRepository = subscriberRepository;
		this.userRepository = userRepository;
	}

	public PageResponse<AffiliateLeadResponse> list(int page, int size, String actorUsername) {
		requireAffiliate(actorUsername);
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		Page<Subscriber> subscribers = subscriberRepository.findByActiveTrue(
				PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdDate")));
		return PageResponse.from(subscribers, AffiliateLeadResponse::from);
	}

	private void requireAffiliate(String username) {
		if (!StringUtils.hasText(username)) {
			throw new IllegalArgumentException("Authenticated username is required");
		}
		TrackerUser user = userRepository.findByUsername(username.trim())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username.trim()));
		if (user.getPrivilege() == null || user.getPrivilege().getName() != PrivilegeRole.Affiliate) {
			throw new IllegalArgumentException("Only affiliate accounts can view affiliate leads");
		}
	}
}
