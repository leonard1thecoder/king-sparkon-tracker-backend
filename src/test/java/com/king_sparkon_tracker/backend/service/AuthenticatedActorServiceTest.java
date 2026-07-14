package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

class AuthenticatedActorServiceTest {

	private TrackerUserRepository userRepository;
	private AuthenticatedActorService service;

	@BeforeEach
	void setUp() {
		userRepository = mock(TrackerUserRepository.class);
		service = new AuthenticatedActorService(userRepository);
	}

	@Test
	void rejectsCrossBusinessAccessForOwner() {
		stubActor("owner", PrivilegeRole.Owner, 10L);

		assertThatThrownBy(() -> service.requireBusinessAccess("owner", 11L))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Cross-business access is forbidden");
	}

	@Test
	void permitsMatchingBusinessAccessForWorker() {
		stubActor("worker", PrivilegeRole.Worker, 10L);

		assertThat(service.requireBusinessAccess("worker", 10L).businessId()).isEqualTo(10L);
	}

	@Test
	void permitsAdministratorAcrossBusinesses() {
		stubActor("admin", PrivilegeRole.Admin, null);

		assertThat(service.requireBusinessAccess("admin", 99L).isAdmin()).isTrue();
	}

	private void stubActor(String username, PrivilegeRole role, Long businessId) {
		TrackerUser user = mock(TrackerUser.class);
		Privilege privilege = new Privilege(role);
		when(user.getId()).thenReturn(1L);
		when(user.getUsername()).thenReturn(username);
		when(user.getPrivilege()).thenReturn(privilege);

		if (businessId != null) {
			Business business = mock(Business.class);
			when(business.getId()).thenReturn(businessId);
			when(user.getBusiness()).thenReturn(business);
		}

		when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
	}
}
