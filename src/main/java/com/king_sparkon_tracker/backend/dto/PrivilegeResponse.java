package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record PrivilegeResponse(
		@Schema(description = "Privilege id.", example = "1") Long id,
		@Schema(description = "Privilege role name.", example = "Owner") PrivilegeRole name) {

	/**
	 * Converts a privilege entity into a small role response.
	 */
	public static PrivilegeResponse from(Privilege privilege) {
		return new PrivilegeResponse(privilege.getId(), privilege.getName());
	}
}
