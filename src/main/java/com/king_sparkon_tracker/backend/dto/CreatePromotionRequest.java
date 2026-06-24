package com.king_sparkon_tracker.backend.dto;

import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.PromotionChannel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePromotionRequest(
		@Schema(description = "Promotion title.", example = "Weekend stock special")
		@NotBlank
		@Size(max = 160)
		String title,

		@Schema(description = "Promotion message sent to subscribers.", example = "Get your inventory tracked properly this weekend.")
		@NotBlank
		@Size(max = 2000)
		String message,

		@Schema(description = "Optional landing URL.", example = "https://kingsparkon.com/pricing")
		@Size(max = 2048)
		String landingUrl,

		@Schema(description = "Preferred sending channel. Defaults to ANY.", example = "ANY")
		PromotionChannel channel,

		@Schema(description = "Optional scheduled time. Defaults to now.")
		OffsetDateTime scheduledFor
) {
}
