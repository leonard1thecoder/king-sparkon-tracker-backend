package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
		@Schema(description = "User id.", example = "1")
		Long id,

		@Schema(description = "Login username.", example = "owner")
		String username,

		@Schema(description = "User email address.", example = "owner@example.com")
		String emailAddress,

		@Schema(description = "User creation timestamp.")
		LocalDateTime createdDate,

		@Schema(description = "Last modification timestamp.")
		LocalDateTime modifiedDate,

		@Schema(description = "User privilege role.", example = "Owner")
		PrivilegeRole privilege,

		@Schema(description = "Business id this user belongs to.", example = "3")
		Long businessId,

		@Schema(description = "Business name this user belongs to.", example = "Owner Retail Store")
		String businessName,

		@Schema(description = "User localization country.", example = "SOUTH_AFRICA")
		LocalizationCountry localizationCountry
) {

	public static UserResponse from(TrackerUser user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmailAddress(),
				user.getCreatedDate(),
				user.getModifiedDate(),
				user.getPrivilege().getName(),
				user.getBusiness() == null ? null : user.getBusiness().getId(),
				user.getBusiness() == null ? null : user.getBusiness().getName(),
				user.getLocalizationCountry()
		);
	}
}
