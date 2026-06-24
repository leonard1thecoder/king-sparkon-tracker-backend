package com.king_sparkon_tracker.backend.dto;

import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.SubscriberType;

import io.swagger.v3.oas.annotations.media.Schema;

public record SubscriberResponse(
		@Schema(description = "Subscriber id.", example = "1")
		Long id,

		@Schema(description = "Email address or cellphone number.", example = "client@example.com")
		String contactValue,

		@Schema(description = "Detected contact type.", example = "EMAIL")
		SubscriberContactType contactType,

		@Schema(description = "Subscriber type.", example = "CLIENT")
		SubscriberType subscriberType,

		@Schema(description = "True when an affiliate subscriber already has an affiliate account.", example = "false")
		boolean affiliateRegistered,

		@Schema(description = "Preferred promotion channel.", example = "ANY")
		PromotionChannel preferredChannel,

		@Schema(description = "Whether this subscriber is active.", example = "true")
		boolean active,

		@Schema(description = "Signup source.", example = "TIP_PAYMENT_LINK")
		String source,

		@Schema(description = "Last time any promotion was sent to this subscriber.")
		OffsetDateTime lastNotifiedAt,

		@Schema(description = "Creation timestamp.")
		OffsetDateTime createdDate
) {

	public static SubscriberResponse from(Subscriber subscriber) {
		return new SubscriberResponse(
				subscriber.getId(),
				subscriber.getContactValue(),
				subscriber.getContactType(),
				subscriber.getSubscriberType(),
				subscriber.isAffiliateRegistered(),
				subscriber.getPreferredChannel(),
				subscriber.isActive(),
				subscriber.getSource(),
				subscriber.getLastNotifiedAt(),
				subscriber.getCreatedDate()
		);
	}
}
