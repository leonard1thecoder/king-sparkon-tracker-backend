package com.king_sparkon_tracker.backend.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.service.PrivilegeService;

class PrivilegeSeederTest {

	@Test
	void runSeedsEveryPrivilegeRole() {
		PrivilegeService privilegeService = mock(PrivilegeService.class);
		PrivilegeSeeder seeder = new PrivilegeSeeder(privilegeService);

		seeder.run(mock(ApplicationArguments.class));

		verify(privilegeService).createPrivilege(PrivilegeRole.Owner);
		verify(privilegeService).createPrivilege(PrivilegeRole.Worker);
	}
}
