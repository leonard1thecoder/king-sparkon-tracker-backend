package com.king_sparkon_tracker.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class TrackerUserDetailsServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	@InjectMocks
	private TrackerUserDetailsService userDetailsService;

	@Test
	void loadUserByUsernameReturnsSpringSecurityUser() {
		TrackerUser user = new TrackerUser(
				"owner",
				"owner@kingsparkon.co.za",
				"encoded-secret",
				new Privilege(PrivilegeRole.Owner));
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user));

		UserDetails result = userDetailsService.loadUserByUsername("owner");

		assertThat(result.getUsername()).isEqualTo("owner");
		assertThat(result.getPassword()).isEqualTo("encoded-secret");
		assertThat(result.getAuthorities())
				.extracting("authority")
				.containsExactlyInAnyOrder("Owner", "ROLE_Owner");
	}

	@Test
	void loadUserByUsernameThrowsWhenMissing() {
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("User not found: missing");
	}
}
