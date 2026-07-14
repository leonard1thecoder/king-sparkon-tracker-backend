package com.king_sparkon_tracker.backend.tickets.service;

import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.service.GoogleStorageService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.FaceDecision;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.GateVerificationRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.GateVerificationResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.TicketIdentityResponse;
import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLevel;
import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLog;
import com.king_sparkon_tracker.backend.tickets.model.UserTicket;
import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import com.king_sparkon_tracker.backend.tickets.repository.TicketAuditLogRepository;
import com.king_sparkon_tracker.backend.tickets.repository.UserTicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class TicketIdentityService {

    private static final long VERIFICATION_PHOTO_URL_MINUTES = 15L;

    private final UserTicketRepository userTicketRepository;
    private final TicketManagementService ticketManagementService;
    private final TicketAuditLogRepository ticketAuditLogRepository;
    private final TrackerUserService trackerUserService;
    private final GoogleStorageService googleStorageService;

    public TicketIdentityService(
            UserTicketRepository userTicketRepository,
            TicketManagementService ticketManagementService,
            TicketAuditLogRepository ticketAuditLogRepository,
            TrackerUserService trackerUserService,
            GoogleStorageService googleStorageService
    ) {
        this.userTicketRepository = userTicketRepository;
        this.ticketManagementService = ticketManagementService;
        this.ticketAuditLogRepository = ticketAuditLogRepository;
        this.trackerUserService = trackerUserService;
        this.googleStorageService = googleStorageService;
    }

    @Transactional(readOnly = true)
    public List<TicketIdentityResponse> getCurrentUserTickets(String actorUsername) {
        TrackerUser actor = actor(actorUsername);
        return userTicketRepository.findByUserIdOrderByPurchasedAtDesc(String.valueOf(actor.getId()))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TicketIdentityResponse saveVerificationPhoto(String ticketId, MultipartFile file, String actorUsername) {
        TrackerUser actor = actor(actorUsername);
        UserTicket ticket = requireOwnedTicket(ticketId, actor);
        requireActive(ticket, "Only ACTIVE tickets can receive a verification photo.");

        GoogleStorageService.StoredImage image = googleStorageService.storePrivateImage(
                file,
                "ticket-verification",
                "ticket-" + ticket.getId() + "-owner-" + ticket.getUserId() + "-v" + ownershipVersion(ticket));

        ticket.setVerificationPhotoObjectName(image.objectName());
        ticket.setVerificationPhotoCapturedAt(Instant.now());
        UserTicket saved = userTicketRepository.save(ticket);

        audit(saved, actor.getUsername(), "TICKET_VERIFICATION_PHOTO_UPDATED", TicketAuditLevel.INFO,
                "Ticket owner captured or replaced the manual gate-verification photo.");
        return toResponse(saved);
    }

    public TicketIdentityResponse transferTicket(String ticketId, String recipientUsername, String actorUsername) {
        TrackerUser sender = actor(actorUsername);
        UserTicket ticket = requireOwnedTicket(ticketId, sender);
        requireActive(ticket, "Only ACTIVE tickets can be shared.");

        TrackerUser recipient = trackerUserService.getUserByUsername(requiredText(recipientUsername, "Recipient username is required"));
        String senderId = String.valueOf(sender.getId());
        String recipientId = String.valueOf(recipient.getId());
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("Ticket is already owned by this username.");
        }

        int nextOwnershipVersion = ownershipVersion(ticket) + 1;
        ticket.setTransferredFromUserId(senderId);
        ticket.setTransferredAt(Instant.now());
        ticket.setUserId(recipientId);
        ticket.setBuyerName(recipient.getUsername());
        ticket.setBuyerEmail(recipient.getEmailAddress());
        ticket.setVerificationPhotoObjectName(null);
        ticket.setVerificationPhotoCapturedAt(null);
        ticket.setOwnershipVersion(nextOwnershipVersion);
        ticket.setQrCodeValue(qrPayload(ticket, nextOwnershipVersion));

        UserTicket saved = userTicketRepository.save(ticket);
        audit(saved, sender.getUsername(), "TICKET_TRANSFERRED", TicketAuditLevel.INFO,
                "Ticket transferred to username " + recipient.getUsername() + "; verification photo reset and QR rotated.");
        return toResponse(saved);
    }

    public GateVerificationResponse verifyByQr(GateVerificationRequest request, String actorUsername) {
        UserTicket ticket = userTicketRepository.findByQrCodeValue(request.value().trim()).orElse(null);
        return verifyAtGate(ticket, request, actorUsername);
    }

    public GateVerificationResponse verifyByReference(GateVerificationRequest request, String actorUsername) {
        UserTicket ticket = userTicketRepository.findByTicketReferenceIgnoreCase(request.value().trim()).orElse(null);
        return verifyAtGate(ticket, request, actorUsername);
    }

    private GateVerificationResponse verifyAtGate(UserTicket ticket, GateVerificationRequest request, String actorUsername) {
        TrackerUser worker = actor(actorUsername);
        requireGateRole(worker);

        if (ticket == null) {
            audit(null, null, null, worker.getUsername(), "TICKET_INVALID", TicketAuditLevel.WARN, "Invalid ticket gate lookup.");
            return new GateVerificationResponse(false, false, "Invalid ticket.", null, null);
        }

        requireWorkerBusinessAccess(worker, ticket);
        TicketEventResponse event = ticketManagementService.getEventById(ticket.getEventId());
        if (ticket.getStatus() != UserTicketStatus.ACTIVE) {
            String message = statusMessage(ticket.getStatus());
            audit(ticket, worker.getUsername(), "TICKET_REJECTED", TicketAuditLevel.WARN, message);
            return new GateVerificationResponse(false, false, message, toResponse(ticket), event);
        }

        if (!StringUtils.hasText(ticket.getVerificationPhotoObjectName())) {
            String message = "Entry blocked: the current ticket owner has not captured a verification photo.";
            audit(ticket, worker.getUsername(), "TICKET_PHOTO_MISSING", TicketAuditLevel.WARN, message);
            return new GateVerificationResponse(false, false, message, toResponse(ticket), event);
        }

        FaceDecision decision = request.faceDecision() == null ? FaceDecision.PENDING : request.faceDecision();
        if (decision == FaceDecision.PENDING) {
            return new GateVerificationResponse(
                    false,
                    true,
                    "Compare the person at the gate with the stored owner photo, then confirm match or deny entry.",
                    toResponse(ticket),
                    event);
        }

        if (decision == FaceDecision.NO_MATCH) {
            String message = "Entry denied: the person does not match the ticket owner verification photo.";
            audit(ticket, worker.getUsername(), "TICKET_FACE_MISMATCH", TicketAuditLevel.WARN, message);
            return new GateVerificationResponse(false, false, message, toResponse(ticket), event);
        }

        ticket.setStatus(UserTicketStatus.USED);
        ticket.setUsedAt(Instant.now());
        ticket.setScannedByWorkerId(worker.getUsername());
        UserTicket saved = userTicketRepository.save(ticket);
        audit(saved, worker.getUsername(), "TICKET_FACE_MATCHED_AND_USED", TicketAuditLevel.INFO,
                "Worker manually confirmed the person matches the stored ticket owner photo; entry approved and ticket marked USED.");

        return new GateVerificationResponse(
                true,
                false,
                "Face confirmed. Entry approved and ticket marked USED.",
                toResponse(saved),
                event);
    }

    private TrackerUser actor(String actorUsername) {
        return trackerUserService.getUserByUsername(requiredText(actorUsername, "Authenticated username is required"));
    }

    private UserTicket requireOwnedTicket(String ticketId, TrackerUser actor) {
        UserTicket ticket = userTicketRepository.findById(requiredText(ticketId, "Ticket id is required"))
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found."));
        if (!String.valueOf(actor.getId()).equals(ticket.getUserId())) {
            throw new IllegalArgumentException("Ticket does not belong to the authenticated user.");
        }
        return ticket;
    }

    private void requireActive(UserTicket ticket, String message) {
        if (ticket.getStatus() != UserTicketStatus.ACTIVE) {
            throw new IllegalStateException(message + " Current status: " + ticket.getStatus() + ".");
        }
    }

    private void requireWorkerBusinessAccess(TrackerUser actor, UserTicket ticket) {
        if (actor.getPrivilege() != null && actor.getPrivilege().getName() == PrivilegeRole.Admin) {
            return;
        }
        if (actor.getBusiness() == null || actor.getBusiness().getId() == null) {
            throw new IllegalArgumentException("Ticket verifier is not linked to a business.");
        }
        if (ticket.getBusinessId() != null && !ticket.getBusinessId().equals(actor.getBusiness().getId())) {
            throw new IllegalArgumentException("Ticket belongs to another business.");
        }
        if (ticket.getBusinessId() == null) {
            TicketEventResponse event = ticketManagementService.getEventById(ticket.getEventId());
            String ownerId = actor.getBusiness().getOwner() == null || actor.getBusiness().getOwner().getId() == null
                    ? null : String.valueOf(actor.getBusiness().getOwner().getId());
            if (ownerId == null || !ownerId.equals(event.ownerId())) {
                throw new IllegalArgumentException("Ticket belongs to another business.");
            }
        }
    }

    private void requireGateRole(TrackerUser actor) {
        if (actor.getPrivilege() == null || actor.getPrivilege().getName() == null) {
            throw new IllegalArgumentException("Worker privilege is required for ticket verification.");
        }
        PrivilegeRole role = actor.getPrivilege().getName();
        if (role != PrivilegeRole.Worker && role != PrivilegeRole.Owner && role != PrivilegeRole.Admin) {
            throw new IllegalArgumentException("Only workers, business owners or administrators can verify ticket entry.");
        }
    }

    private TicketIdentityResponse toResponse(UserTicket ticket) {
        boolean active = ticket.getStatus() == UserTicketStatus.ACTIVE;
        String photoUrl = StringUtils.hasText(ticket.getVerificationPhotoObjectName())
                ? googleStorageService.signedReadUrl(ticket.getVerificationPhotoObjectName(), VERIFICATION_PHOTO_URL_MINUTES)
                : null;

        return new TicketIdentityResponse(
                ticket.getId(),
                ticket.getEventId(),
                ticket.getUserId(),
                ticket.getBuyerName(),
                ticket.getBuyerEmail(),
                ticket.getTicketType(),
                ticket.getPricePaid(),
                ticket.getQrCodeValue(),
                ticket.getTicketReference(),
                ticket.getStatus(),
                ticket.getPurchasedAt(),
                ticket.getUsedAt(),
                ticket.getScannedByWorkerId(),
                photoUrl,
                ticket.getVerificationPhotoCapturedAt(),
                !StringUtils.hasText(ticket.getVerificationPhotoObjectName()),
                active,
                active,
                ticket.getTransferredAt(),
                ticket.getTransferredFromUserId(),
                ownershipVersion(ticket));
    }

    private int ownershipVersion(UserTicket ticket) {
        return ticket.getOwnershipVersion() == null || ticket.getOwnershipVersion() < 1 ? 1 : ticket.getOwnershipVersion();
    }

    private String qrPayload(UserTicket ticket, int ownershipVersion) {
        return "KST-TICKET:ticketId=" + ticket.getId()
                + ";eventId=" + ticket.getEventId()
                + ";ticketReference=" + ticket.getTicketReference()
                + ";userId=" + ticket.getUserId()
                + ";ownershipVersion=" + ownershipVersion;
    }

    private String statusMessage(UserTicketStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Ticket payment is still pending. Entry denied.";
            case USED -> "Ticket already used. Entry denied.";
            case CANCELLED -> "Ticket cancelled. Entry denied.";
            case EXPIRED -> "Ticket expired. Entry denied.";
            case ACTIVE -> "Ticket is active.";
        };
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private void audit(UserTicket ticket, String actorId, String action, TicketAuditLevel level, String message) {
        audit(
                ticket == null ? null : ticketManagementService.getEventById(ticket.getEventId()).ownerId(),
                ticket == null ? null : ticket.getEventId(),
                ticket == null ? null : ticket.getId(),
                actorId,
                action,
                level,
                message);
    }

    private void audit(String ownerId, String eventId, String ticketId, String actorId, String action, TicketAuditLevel level, String message) {
        TicketAuditLog log = new TicketAuditLog();
        log.setId("ticket-log-" + java.util.UUID.randomUUID());
        log.setOwnerId(ownerId);
        log.setEventId(eventId);
        log.setTicketId(ticketId);
        log.setActorId(actorId);
        log.setAction(action);
        log.setLevel(level);
        log.setMessage(message);
        log.setStructuredMetadata(
                "ownerId=" + ownerId
                        + ",eventId=" + eventId
                        + ",ticketId=" + ticketId
                        + ",actorId=" + actorId
                        + ",action=" + action.toUpperCase(Locale.ROOT));
        ticketAuditLogRepository.save(log);
    }
}
