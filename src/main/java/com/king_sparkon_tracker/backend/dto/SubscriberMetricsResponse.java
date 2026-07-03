package com.king_sparkon_tracker.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubscriberMetricsResponse(
		@Schema(description = "Active subscribers.", example = "856")
		long activeSubscribers,
		@Schema(description = "Active email subscribers.", example = "620")
		long emailSubscribers,
		@Schema(description = "Active cellphone subscribers.", example = "236")
		long cellphoneSubscribers,
		@Schema(description = "Active registered affiliate subscribers.", example = "42")
		long affiliateRegisteredSubscribers
) {
}
