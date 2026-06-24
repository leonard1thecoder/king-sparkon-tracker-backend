package com.king_sparkon_tracker.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAdministratorRequest(
		@Schema(description = "Administrator login username.", example = "admin")
		@NotBlank
		String username,

		@Schema(description = "Administrator email address. Must use the kingsparkon.com domain.", example = "admin@kingsparkon.com")
		@NotBlank
		@Email
		String emailAddress,

		@Schema(description = "Administrator password.", example = "secret")
		@NotBlank
		String password,

		@JsonAlias({ "LocalizationContry", "localizationContry" })
		@Schema(description = "Administrator localization country.", example = "SOUTH_AFRICA")
		LocalizationCountry localizationCountry,

		@Schema(description = "Administrator physical address captured during onboarding.", example = "12 King Sparkon Road, Johannesburg")
		@NotBlank
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "Administrator cellphone number captured during onboarding.", example = "+27821234567")
		@NotBlank
		@Size(max = 32)
		String cellphoneNumber
) {
}
