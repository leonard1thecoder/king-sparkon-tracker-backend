package com.king_sparkon_tracker.backend.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.PrivilegeService;

@Component
public class PrivilegeSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PrivilegeSeeder.class);

	private final PrivilegeService privilegeService;

	public PrivilegeSeeder(PrivilegeService privilegeService) {
		this.privilegeService = privilegeService;
	}

	/**
	 * Ensures required owner and worker roles exist before users are created or authenticated.
	 */
	@Override
	public void run(ApplicationArguments args) {
		for (PrivilegeRole role : PrivilegeRole.values()) {
			privilegeService.createPrivilege(role);
		}
		log.info("privileges_seeded roles={}", java.util.Arrays.toString(PrivilegeRole.values()));
	}
}
