package com.king_sparkon_tracker.backend.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
public class TrackerUserDetailsService implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(TrackerUserDetailsService.class);

	private final TrackerUserRepository userRepository;

	public TrackerUserDetailsService(TrackerUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Bridges persisted tracker users into Spring Security's authentication model.
	 */
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		TrackerUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> {
					log.warn("security_user_lookup_failed username={}", username);
					return new UsernameNotFoundException("User not found: " + username);
				});
		log.debug("security_user_loaded userId={} username={} role={}", user.getId(), username, user.getPrivilege().getName());
		return User.builder()
				.username(user.getUsername())
				.password(user.getPassword())
				.authorities(authoritiesFor(user))
				.build();
	}

	/**
	 * Exposes both raw and ROLE-prefixed authorities for compatibility with Spring Security matchers.
	 */
	private List<GrantedAuthority> authoritiesFor(TrackerUser user) {
		String role = user.getPrivilege().getName().name();
		return List.of(
				new SimpleGrantedAuthority(role),
				new SimpleGrantedAuthority("ROLE_" + role));
	}
}
