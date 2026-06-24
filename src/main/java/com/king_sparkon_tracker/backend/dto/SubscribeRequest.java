package com.king_sparkon_tracker.backend.dto;

import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.SubscriberType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubscribeRequest(
		@Schema(description = "Subscriber email address or cellphone number.", example = "client@example.com")
		@NotBlank
		@Size(max = 320)
		String contact,

		@Schema(description = "Subscriber type. Use AFFILIATE for affiliate-program subscription leads.", example = "KINGSPARKON_SUBSCRIBER")
		SubscriberType subscriberType,

		@Schema(description = "True when this affiliate subscriber already has a system affiliate account.", example = "false")
		Boolean affiliateRegistered,

		@Schema(description = "Preferred promotion channel.", example = "ANY")
		PromotionChannel preferredChannel
) {
}
