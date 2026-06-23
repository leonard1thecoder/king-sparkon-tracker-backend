package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@Schema(description = "Owner login username.", example = "owner")
		@NotBlank
		String username,

		@Schema(description = "Owner email address.", example = "owner@example.com")
		@NotBlank
		@Email
		String emailAddress,

		@Schema(description = "Owner password.", example = "secret")
		@NotBlank
		String password,

		@Schema(description = "Business name this owner is registering.", example = "Owner Retail Store")
		@NotBlank
		String businessName,

		@JsonAlias({ "LocalizationContry", "localizationContry" })
		@Schema(description = "User localization country.", example = "SOUTH_AFRICA")
		LocalizationCountry localizationCountry,

		@Schema(description = "Owner physical business or residential address used for onboarding.", example = "12 Main Road, Johannesburg")
		@Size(max = 1024)
		String physicalAddress,

		@Schema(description = "Owner cellphone number used for onboarding and account contact.", example = "+27821234567")
		@Size(max = 32)
		String cellphoneNumber
) {
	public RegisterUserRequest(
			String username,
			String emailAddress,
			String password,
			String businessName,
			LocalizationCountry localizationCountry) {
		this(username, emailAddress, password, businessName, localizationCountry, null, null);
	}
}
