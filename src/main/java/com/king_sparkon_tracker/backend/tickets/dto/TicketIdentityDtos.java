package com.king_sparkon_tracker.backend.tickets.dto;

import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class TicketIdentityDtos {
    private TicketIdentityDtos() {}

    public enum FaceDecision {
        PENDING,
        MATCH,
        NO_MATCH
    }

    public record TransferTicketRequest(
            @NotBlank @Size(max = 100) String username
    ) {}

    public record GateVerificationRequest(
            @NotBlank String value,
            String workerId,
            FaceDecision faceDecision
    ) {}

    public record TicketIdentityResponse(
            String id,
            String eventId,
            String userId,
            String buyerName,
            String buyerEmail,
            TicketType ticketType,
            BigDecimal pricePaid,
            String qrCodeValue,
            String ticketReference,
            UserTicketStatus status,
            Instant purchasedAt,
            Instant usedAt,
            String scannedByWorkerId,
            String verificationPhotoUrl,
            Instant verificationPhotoCapturedAt,
            boolean verificationRequired,
            boolean canShare,
            boolean canChangeVerificationPhoto,
            Instant transferredAt,
            String transferredFromUserId,
            int ownershipVersion
    ) {}

    public record GateVerificationResponse(
            boolean valid,
            boolean requiresFaceConfirmation,
            String message,
            TicketIdentityResponse ticket,
            TicketEventResponse event
    ) {}
}
