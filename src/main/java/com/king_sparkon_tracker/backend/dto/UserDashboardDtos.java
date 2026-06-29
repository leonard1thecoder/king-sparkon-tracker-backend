package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;

public final class UserDashboardDtos {
	private UserDashboardDtos() {
	}

	public record BusinessCardResponse(
			Long businessId,
			String businessName,
			String description,
			String qrCodeUrl,
			Long ownerId,
			String ownerUsername,
			String ownerProfilePictureUrl
	) {
		public static BusinessCardResponse from(Business business) {
			TrackerUser owner = business.getOwner();
			return new BusinessCardResponse(
					business.getId(),
					business.getName(),
					business.getDescription(),
					business.getQrCodeUrl(),
					owner == null ? null : owner.getId(),
					owner == null ? null : owner.getUsername(),
					owner == null ? null : owner.getProfilePictureUrl());
		}
	}

	public record WorkerTipCardResponse(
			Long workerId,
			String username,
			String emailAddress,
			String jobTitle,
			String profilePictureUrl,
			boolean tipQrCodeEnabled,
			String tipQrCodeUrl
	) {
		public static WorkerTipCardResponse from(TrackerUser worker) {
			return new WorkerTipCardResponse(
					worker.getId(),
					worker.getUsername(),
					worker.getEmailAddress(),
					worker.getJobTitle(),
					worker.getProfilePictureUrl(),
					worker.isTipQrCodeEnabled(),
					worker.getTipQrCodeUrl());
		}
	}

	public record UserDashboardResponse(
			List<BusinessCardResponse> businesses,
			List<TicketEventResponse> upcomingEvents
	) {
	}
}
