package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.AdminOverviewResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional(readOnly = true)
public class AdministratorService {

	private static final Logger log = LoggerFactory.getLogger(AdministratorService.class);
	private static final String ADMIN_ACCESS_MESSAGE = "Only administrators can access platform administration";

	private final TrackerUserRepository userRepository;
	private final BusinessRepository businessRepository;

	public AdministratorService(
			TrackerUserRepository userRepository,
			BusinessRepository businessRepository) {
		this.userRepository = userRepository;
		this.businessRepository = businessRepository;
	}

	public AdminOverviewResponse overview(String actorUsername) {
		TrackerUser administrator = requireAdministrator(actorUsername);
		log.debug("admin_overview_requested adminId={} username={}", administrator.getId(), administrator.getUsername());

		return new AdminOverviewResponse(
				userRepository.count(),
				userRepository.countByPrivilege_Name(PrivilegeRole.Admin),
				userRepository.countByPrivilege_Name(PrivilegeRole.Owner),
				userRepository.countByPrivilege_Name(PrivilegeRole.Worker),
				userRepository.countByPrivilege_Name(PrivilegeRole.Affiliate),
				businessRepository.count()
		);
	}

	public Page<TrackerUser> listUsers(Pageable pageable, String actorUsername) {
		TrackerUser administrator = requireAdministrator(actorUsername);
		log.debug(
				"admin_users_list_requested adminId={} username={} page={} size={}",
				administrator.getId(),
				administrator.getUsername(),
				pageable.getPageNumber(),
				pageable.getPageSize());
		return userRepository.findAll(pageable);
	}

	public TrackerUser getUserById(Long id, String actorUsername) {
		requireAdministrator(actorUsername);
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	public Page<Business> listBusinesses(Pageable pageable, String actorUsername) {
		TrackerUser administrator = requireAdministrator(actorUsername);
		log.debug(
				"admin_businesses_list_requested adminId={} username={} page={} size={}",
				administrator.getId(),
				administrator.getUsername(),
				pageable.getPageNumber(),
				pageable.getPageSize());
		return businessRepository.findAll(pageable);
	}

	public Business getBusinessById(Long id, String actorUsername) {
		requireAdministrator(actorUsername);
		return businessRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
	}

	private TrackerUser requireAdministrator(String actorUsername) {
		TrackerUser actor = userRepository.findByUsername(actorUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));

		if (actor.getPrivilege() == null || actor.getPrivilege().getName() != PrivilegeRole.Admin) {
			log.warn("admin_access_rejected username={} role={}", actorUsername, actor.getPrivilege() == null ? null : actor.getPrivilege().getName());
			throw new IllegalArgumentException(ADMIN_ACCESS_MESSAGE);
		}

		return actor;
	}
}
