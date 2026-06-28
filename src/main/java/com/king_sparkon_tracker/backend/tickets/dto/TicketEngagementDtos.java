package com.king_sparkon_tracker.backend.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class TicketEngagementDtos {
    private TicketEngagementDtos() {}

    public record CommentRequest(@NotBlank String eventId, @NotBlank String displayName, @NotBlank String comment) {}
    public record CommentResponse(String id, String eventId, String userId, String displayName, String comment, Instant createdAt) {}
    public record FollowRequest(@NotBlank String businessId) {}
    public record FollowResponse(String businessId, String userId, boolean following) {}
    public record BusinessCatalogItem(String businessId, String businessName, String imagePath, boolean following, long eventCount) {}
}
