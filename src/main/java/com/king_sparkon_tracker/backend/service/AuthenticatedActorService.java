package com.king_sparkon_tracker.backend.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.security.AuthenticatedActor;

@Service
@Transactional(readOnly = true)
public class AuthenticatedActorService {

	private final TrackerUserRepository userRepository;

	public AuthenticatedActorService(TrackerUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public AuthenticatedActor current(String username) {
		if (username == null || username.isBlank()) {
			throw new AccessDeniedException("Authenticated principal is required");
		}

		TrackerUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
		PrivilegeRole role = user.getPrivilege() == null ? null : user.getPrivilege().getName();
		if (role == null) {
			throw new AccessDeniedException("Authenticated user has no assigned role");
		}

		Long businessId = user.getBusiness() == null ? null : user.getBusiness().getId();
		return new AuthenticatedActor(user.getId(), user.getUsername(), role, businessId);
	}

	public AuthenticatedActor requireBusinessAccess(String username, Long resourceBusinessId) {
		AuthenticatedActor actor = current(username);
		if (actor.isAdmin()) {
			return actor;
		}
		if (resourceBusinessId == null || !actor.requireBusinessId().equals(resourceBusinessId)) {
			throw new AccessDeniedException("Cross-business access is forbidden");
		}
		return actor;
	}
}
