package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.TipStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTipStatusRequest(
		@NotNull
		TipStatus status
) {
}
