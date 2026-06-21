package com.king_sparkon_tracker.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.PrivilegeRepository;

@Service
@Transactional
public class PrivilegeService {

	private static final Logger log = LoggerFactory.getLogger(PrivilegeService.class);

	private final PrivilegeRepository privilegeRepository;

	public PrivilegeService(PrivilegeRepository privilegeRepository) {
		this.privilegeRepository = privilegeRepository;
	}

	/**
	 * Creates a role only when it is missing so startup seeding remains idempotent.
	 */
	public Privilege createPrivilege(PrivilegeRole role) {
		if (role == null) {
			throw new IllegalArgumentException("Privilege role is required");
		}
		return privilegeRepository.findByName(role)
				.orElseGet(() -> {
					log.info("privilege_created role={}", role);
					return privilegeRepository.save(new Privilege(role));
				});
	}

	/**
	 * Looks up one privilege by role name for authorization and administration responses.
	 */
	@Transactional(readOnly = true)
	public Privilege getPrivilege(PrivilegeRole role) {
		return privilegeRepository.findByName(role)
				.orElseThrow(() -> new ResourceNotFoundException("Privilege not found: " + role));
	}

	/**
	 * Returns all privileges currently available to the application.
	 */
	@Transactional(readOnly = true)
	public List<Privilege> listPrivileges() {
		log.debug("privileges_list_requested");
		return privilegeRepository.findAll();
	}
}
