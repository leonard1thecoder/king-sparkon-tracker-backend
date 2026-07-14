package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.TicketItem;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.tickets.config.TicketProperties;
import com.king_sparkon_tracker.backend.tickets.domain.TicketBusinessRules;
import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLevel;
import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLog;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.UserTicket;
import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import com.king_sparkon_tracker.backend.tickets.repository.EventTicketTypeRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketAuditLogRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketPaymentRepository;
import com.king_sparkon_tracker.backend.tickets.repository.UserTicketRepository;
import com.king_sparkon_tracker.backend.tickets.reservation.TicketReservationService;

@Service
@Transactional
public class EmbeddedTicketFulfilmentService {

    private static final String STRIPE_PROVIDER = "STRIPE";

    private final TicketEventRepository ticketEventRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final TicketPaymentRepository ticketPaymentRepository;
    private final UserTicketRepository userTicketRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;
    private final TicketProperties ticketProperties;
    private final BusinessRepository businessRepository;
    private final TrackerUserService trackerUserService;
    private final TicketReservationService ticketReservationService;

    public EmbeddedTicketFulfilmentService(
            TicketEventRepository ticketEventRepository,
            EventTicketTypeRepository eventTicketTypeRepository,
            TicketPaymentRepository ticketPaymentRepository,
            UserTicketRepository userTicketRepository,
            TicketAuditLogRepository ticketAuditLogRepository,
            TicketProperties ticketProperties,
            BusinessRepository businessRepository,
            TrackerUserService trackerUserService,
            TicketReservationService ticketReservationService) {
        this.ticketEventRepository = ticketEventRepository;
        this.eventTicketTypeRepository = eventTicketTypeRepository;
        this.ticketPaymentRepository = ticketPaymentRepository;
        this.userTicketRepository = userTicketRepository;
        this.ticketAuditLogRepository = ticketAuditLogRepository;
        this.ticketProperties = ticketProperties;
        this.businessRepository = businessRepository;
        this.trackerUserService = trackerUserService;
        this.ticketReservationService = ticketReservationService;
    }

    public String reserve(
            TicketItem item,
            TrackerUser actor,
            String buyerName,
            String buyerEmail,
            String paymentIntentId) {
        TicketPayment existing = ticketPaymentRepository
                .findByPaymentReferenceAndEventIdAndTicketType(paymentIntentId, item.eventId(), item.ticketType())
                .orElse(null);
        if (existing != null) {
            return existing.getId();
        }

        TicketEvent event = requirePublishedEvent(item.eventId());
        EventTicketType ticketType = eventTicketTypeRepository.findLockedByEventIdAndType(event.getId(), item.ticketType())
                .orElseThrow(() -> new IllegalArgumentException("Ticket type is unavailable for event: " + event.getName()));
        TicketBusinessRules.requirePurchaseCapacity(
                ticketType.getCapacity(),
                ticketType.getSold() + ticketType.getReserved(),
                item.quantity());

        BigDecimal subtotal = TicketBusinessRules.money(ticketType.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        BigDecimal checkoutFee = TicketBusinessRules.calculatePercentAmount(subtotal, ticketProperties.checkoutServiceFeePercent());
        BigDecimal total = TicketBusinessRules.money(subtotal.add(checkoutFee));
        String actorId = String.valueOf(actor.getId());
        Business business = businessForEvent(event);

        TicketPayment payment = new TicketPayment();
        payment.setId(newId("ticket-payment"));
        payment.setEventId(event.getId());
        payment.setBusinessId(business == null ? event.getBusinessId() : business.getId());
        payment.setUserId(actorId);
        payment.setBuyerName(buyerName.trim());
        payment.setBuyerEmail(buyerEmail.trim().toLowerCase());
        payment.setTicketType(item.ticketType());
        payment.setQuantity(item.quantity());
        payment.setSubtotalAmount(subtotal);
        payment.setCheckoutServiceFeeAmount(checkoutFee);
        payment.setTotalAmount(total);
        payment.setStatus(TicketPaymentStatus.PENDING);
        payment.setPaymentProvider(STRIPE_PROVIDER);
        payment.setPaymentReference(paymentIntentId);
        TicketPayment savedPayment = ticketPaymentRepository.save(payment);

        BigDecimal perTicket = TicketBusinessRules.money(subtotal.divide(BigDecimal.valueOf(item.quantity()), 2, RoundingMode.HALF_UP));
        List<UserTicket> pendingTickets = new ArrayList<>();
        for (int index = 0; index < item.quantity(); index++) {
            UserTicket ticket = new UserTicket();
            ticket.setId(newId("ticket"));
            ticket.setEventId(event.getId());
            ticket.setBusinessId(payment.getBusinessId());
            ticket.setPaymentId(savedPayment.getId());
            ticket.setUserId(actorId);
            ticket.setBuyerName(buyerName.trim());
            ticket.setBuyerEmail(buyerEmail.trim().toLowerCase());
            ticket.setTicketType(item.ticketType());
            ticket.setPricePaid(perTicket);
            ticket.setTicketReference(nextReference(event.getId()));
            ticket.setStatus(UserTicketStatus.PENDING_PAYMENT);
            ticket.setQrCodeValue(null);
            pendingTickets.add(ticket);
        }
        userTicketRepository.saveAll(pendingTickets);
        ticketReservationService.reserve(business, ticketType, savedPayment, actorId, item.quantity());
        audit(event, actorId, paymentIntentId, savedPayment.getId(), "TICKETS_EMBEDDED_PAYMENT_RESERVED",
                item.quantity() + " " + item.ticketType() + " tickets reserved for embedded payment");
        return savedPayment.getId();
    }

    public String fulfil(
            TicketItem item,
            TrackerUser actor,
            String buyerName,
            String buyerEmail,
            String paymentIntentId) {
        TicketPayment payment = ticketPaymentRepository
                .findByPaymentReferenceAndEventIdAndTicketType(paymentIntentId, item.eventId(), item.ticketType())
                .orElseGet(() -> {
                    String paymentId = reserve(item, actor, buyerName, buyerEmail, paymentIntentId);
                    return ticketPaymentRepository.findById(paymentId)
                            .orElseThrow(() -> new IllegalStateException("Embedded ticket payment disappeared"));
                });
        ticketReservationService.confirm(payment.getId(), paymentIntentId);
        TicketEvent event = requirePublishedEvent(item.eventId());
        audit(event, String.valueOf(actor.getId()), paymentIntentId, payment.getId(), "TICKETS_EMBEDDED_PAYMENT_FULFILLED",
                item.quantity() + " " + item.ticketType() + " tickets activated after Stripe PaymentIntent success");
        return payment.getId();
    }

    public void releaseAll(String paymentIntentId) {
        ticketPaymentRepository.findAllByPaymentReferenceOrderByCreatedAtAsc(paymentIntentId).stream()
                .filter(payment -> payment.getStatus() == TicketPaymentStatus.PENDING)
                .forEach(payment -> ticketReservationService.release(payment.getId(), false));
    }

    private TicketEvent requirePublishedEvent(String eventId) {
        TicketEvent event = ticketEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket event not found: " + eventId));
        if (event.getStatus() != TicketEventStatus.PUBLISHED) {
            throw new IllegalStateException("Event is not published for ticket sales: " + event.getName());
        }
        return event;
    }

    private Business businessForEvent(TicketEvent event) {
        if (event.getBusinessId() != null) {
            return businessRepository.findById(event.getBusinessId())
                    .orElseThrow(() -> new IllegalStateException("Ticket event business no longer exists"));
        }
        try {
            TrackerUser owner = trackerUserService.getUserById(Long.valueOf(event.getOwnerId()));
            return owner.getBusiness();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void audit(TicketEvent event, String actorId, String paymentIntentId, String ticketPaymentId, String action, String message) {
        TicketAuditLog auditLog = new TicketAuditLog();
        auditLog.setId(newId("ticket-log"));
        auditLog.setOwnerId(event.getOwnerId());
        auditLog.setEventId(event.getId());
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setLevel(TicketAuditLevel.INFO);
        auditLog.setMessage(message);
        auditLog.setStructuredMetadata("ownerId=" + event.getOwnerId()
                + ",eventId=" + event.getId()
                + ",actorId=" + actorId
                + ",paymentIntentId=" + paymentIntentId
                + ",ticketPaymentId=" + ticketPaymentId);
        ticketAuditLogRepository.save(auditLog);
    }

    private String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String nextReference(String eventId) {
        String normalized = eventId == null ? "EVENT" : eventId.replace("event-", "");
        String prefix = normalized.substring(0, Math.min(8, normalized.length())).toUpperCase();
        if (prefix.isBlank()) prefix = "EVENT";
        return "KST-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
