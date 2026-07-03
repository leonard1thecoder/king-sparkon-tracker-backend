package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserEmailVerificationStatusResponse(
		@Schema(description = "User id.", example = "1")
		Long userId,

		@Schema(description = "Login username.", example = "owner")
		String username,

		@Schema(description = "User email address.", example = "owner@example.com")
		String emailAddress,

		@Schema(description = "User privilege role.", example = "Owner")
		PrivilegeRole privilege,

		@Schema(description = "Whether the user's email has been verified or manually bypassed.", example = "true")
		boolean emailVerified,

		@Schema(description = "Timestamp when the email verification status was set to verified.")
		LocalDateTime emailVerifiedAt,

		@Schema(description = "Admin username that performed the status update.", example = "admin")
		String updatedBy
) {

	public static UserEmailVerificationStatusResponse from(TrackerUser user, String updatedBy) {
		return new UserEmailVerificationStatusResponse(
				user.getId(),
				user.getUsername(),
				user.getEmailAddress(),
				user.getPrivilege().getName(),
				user.isEmailVerified(),
				user.getEmailVerifiedAt(),
				updatedBy
		);
	}
}
