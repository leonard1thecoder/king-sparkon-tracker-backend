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

		@Schema(description = "Subscriber type. Defaults to KINGSPARKON_SUBSCRIBER for direct signups.", example = "KINGSPARKON_SUBSCRIBER")
		SubscriberType subscriberType,

		@Schema(description = "Preferred promotion channel.", example = "ANY")
		PromotionChannel preferredChannel
) {
}
