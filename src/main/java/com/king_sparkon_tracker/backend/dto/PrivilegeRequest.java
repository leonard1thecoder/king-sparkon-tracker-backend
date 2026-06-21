package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record PrivilegeRequest(
		@Schema(description = "Privilege enum name.", example = "Owner") @NotNull PrivilegeRole name) {
}
