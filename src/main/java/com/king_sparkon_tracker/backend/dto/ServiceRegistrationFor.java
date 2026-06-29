package com.king_sparkon_tracker.backend.dto;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public registration privilege selector used by the registration page.")
public enum ServiceRegistrationFor {
	USER(PrivilegeRole.User),
	BUSINESS_OWNER(PrivilegeRole.Owner),
	AFFILIATE(PrivilegeRole.Affiliate);

	private final PrivilegeRole privilegeRole;

	ServiceRegistrationFor(PrivilegeRole privilegeRole) {
		this.privilegeRole = privilegeRole;
	}

	public PrivilegeRole privilegeRole() {
		return privilegeRole;
	}

	@JsonCreator
	public static ServiceRegistrationFor from(String value) {
		if (value == null || value.isBlank()) {
			return BUSINESS_OWNER;
		}

		String normalized = value.trim()
				.toUpperCase(Locale.ROOT)
				.replace('-', '_')
				.replace(' ', '_');

		return switch (normalized) {
			case "USER", "CUSTOMER", "CLIENT" -> USER;
			case "OWNER", "BUSINESS", "BUSINESSOWNER", "BUSINESS_OWNER" -> BUSINESS_OWNER;
			case "AFFILIATE", "AFFLIATE" -> AFFILIATE;
			default -> throw new IllegalArgumentException("Unsupported registration privilege: " + value);
		};
	}

	@JsonValue
	public String value() {
		return name();
	}
}
