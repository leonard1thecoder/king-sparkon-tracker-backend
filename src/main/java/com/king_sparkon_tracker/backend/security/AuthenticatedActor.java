package com.king_sparkon_tracker.backend.security;

import org.springframework.security.access.AccessDeniedException;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;

/**
 * Immutable authenticated identity resolved from the server-side principal.
 * Tenant identifiers are never accepted from request payloads as authority.
 */
public record AuthenticatedActor(
		Long userId,
		String username,
		PrivilegeRole role,
		Long businessId) {

	public boolean isAdmin() {
		return role == PrivilegeRole.Admin;
	}

	public Long requireBusinessId() {
		if (businessId == null) {
			throw new AccessDeniedException("Authenticated user is not linked to a business");
		}
		return businessId;
	}
}
