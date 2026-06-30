package com.king_sparkon_tracker.backend.tickets.service;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;
import com.king_sparkon_tracker.backend.service.BusinessAccountService;
import com.king_sparkon_tracker.backend.service.StripeService;
import com.king_sparkon_tracker.backend.service.StripeService.CreatedTicketPaymentLink;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import com.king_sparkon_tracker.backend.tickets.config.TicketProperties;
import com.king_sparkon_tracker.backend.tickets.domain.TicketBusinessRules;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.CreateEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.EventTicketTypeRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.EventTicketTypeResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.OwnerTicketDashboardResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PromoteTicketEventRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.PurchaseTicketsRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketAuditLogResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventPromotionResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketPaymentResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketPurchaseResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketVerificationResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketWithdrawalRequest;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketWithdrawalResponse;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.UpdateEventRequest;
import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLevel;
import com.king_sparkon_tracker.backend.tickets.model.TicketAuditLog;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventPromotion;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPromotionStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketWithdrawal;
import com.king_sparkon_tracker.backend.tickets.model.TicketWithdrawalStatus;
import com.king_sparkon_tracker.backend.tickets.model.UserTicket;
import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import com.king_sparkon_tracker.backend.tickets.repository.EventTicketTypeRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketAuditLogRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventBoostRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketPaymentRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketWithdrawalRepository;
import com.king_sparkon_tracker.backend.tickets.repository.UserTicketRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketManagementService {

    private static final Logger log = LoggerFactory.getLogger(TicketManagementService.class);
    private static final String STRIPE_PROVIDER = "STRIPE";

    private final TicketEventRepository ticketEventRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final UserTicketRepository userTicketRepository;
    private final TicketPaymentRepository ticketPaymentRepository;
    private final TicketWithdrawalRepository ticketWithdrawalRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;
    private final TicketEventBoostRepository ticketEventBoostRepository;
    private final BusinessAccountService businessAccountService;
    private final TrackerUserService trackerUserService;
    private final StripeService stripeService;
    private final TicketProperties ticketProperties;

    public TicketManagementService(
            TicketEventRepository ticketEventRepository,
            EventTicketTypeRepository eventTicketTypeRepository,
            UserTicketRepository userTicketRepository,
            TicketPaymentRepository ticketPaymentRepository,
            TicketWithdrawalRepository ticketWithdrawalRepository,
            TicketAuditLogRepository ticketAuditLogRepository,
            TicketEventBoostRepository ticketEventBoostRepository,
            BusinessAccountService businessAccountService,
            TrackerUserService trackerUserService,
            StripeService stripeService,
            TicketProperties ticketProperties
    ) {
        this.ticketEventRepository = ticketEventRepository;
        this.eventTicketTypeRepository = eventTicketTypeRepository;
        this.userTicketRepository = userTicketRepository;
        this.ticketPaymentRepository = ticketPaymentRepository;
        this.ticketWithdrawalRepository = ticketWithdrawalRepository;
        this.ticketAuditLogRepository = ticketAuditLogRepository;
        this.ticketEventBoostRepository = ticketEventBoostRepository;
        this.businessAccountService = businessAccountService;
        this.trackerUserService = trackerUserService;
        this.stripeService = stripeService;
        this.ticketProperties = ticketProperties;
    }

    @Transactional(readOnly = true)
    public List<TicketEventResponse> getUpcomingEvents() {
        return ticketEventRepository
                .findByStatusInAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(List.of(TicketEventStatus.PUBLISHED, TicketEventStatus.DRAFT), LocalDate.now())
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketEventResponse getEventById(String eventId) {
        return toEventResponse(findEvent(eventId));
    }

    @Transactional(readOnly = true)
    public List<UserTicketResponse> getMyTickets(String userId) {
        return userTicketRepository.findByUserIdOrderByPurchasedAtDesc(userId)
                .stream()
                .map(this::toUserTicketResponse)
                .toList();
    }

    public TicketEventResponse createEvent(CreateEventRequest request) {
        requireTicketTypes(request.ticketTypes());
        TicketEvent event = new TicketEvent();
        event.setId(newId("event"));
        event.setOwnerId(request.ownerId());
        event.setName(request.name().trim());
        event.setDescription(request.description().trim());
        event.setLocation(request.location().trim());
        event.setEventDate(request.eventDate());
        event.setEventTime(request.eventTime());
        event.setBannerUrl(blankToNull(request.bannerUrl()));
        event.setPosterPhotoUrl(blankToNull(request.posterPhotoUrl()));
        event.setWorkerIds(copyIds(request.workerIds()));
        event.setAffiliateIds(copyIds(request.affiliateIds()));
        event.setStatus(request.status());

        request.ticketTypes().stream()
                .sorted(Comparator.comparing(ticketTypeRequest -> canonicalTicketType(ticketTypeRequest.type())))
                .forEach(ticketTypeRequest -> event.addTicketType(toTicketTypeEntity(event.getId(), ticketTypeRequest)));

        TicketEvent saved = ticketEventRepository.save(event);
        audit(saved.getOwnerId(), saved.getId(), null, saved.getOwnerId(), "EVENT_CREATED", TicketAuditLevel.INFO, "Ticket event created: " + saved.getName());
        return toEventResponse(saved);
    }

    public TicketEventResponse updateEvent(String eventId, UpdateEventRequest request) {
        TicketEvent event = findEvent(eventId);
        if (request.name() != null && !request.name().isBlank()) event.setName(request.name().trim());
        if (request.description() != null && !request.description().isBlank()) event.setDescription(request.description().trim());
        if (request.location() != null && !request.location().isBlank()) event.setLocation(request.location().trim());
        if (request.eventDate() != null) event.setEventDate(request.eventDate());
        if (request.eventTime() != null) event.setEventTime(request.eventTime());
        if (request.bannerUrl() != null) event.setBannerUrl(blankToNull(request.bannerUrl()));
        if (request.posterPhotoUrl() != null) event.setPosterPhotoUrl(blankToNull(request.posterPhotoUrl()));
        if (request.workerIds() != null) event.setWorkerIds(copyIds(request.workerIds()));
        if (request.affiliateIds() != null) event.setAffiliateIds(copyIds(request.affiliateIds()));
        if (request.status() != null) event.setStatus(request.status());

        if (request.ticketTypes() != null && !request.ticketTypes().isEmpty()) {
            Map<TicketType, EventTicketType> currentByType = event.getTicketTypes().stream().collect(Collectors.toMap(EventTicketType::getType, Function.identity()));
            for (EventTicketTypeRequest ticketTypeRequest : request.ticketTypes()) {
                TicketType requestedType = canonicalTicketType(ticketTypeRequest.type());
                EventTicketType current = currentByType.get(requestedType);
                if (current == null) continue;
                if (ticketTypeRequest.capacity() < current.getSold()) {
                    throw new IllegalArgumentException("Capacity cannot be lower than tickets already sold for " + requestedType);
                }
                current.setPrice(TicketBusinessRules.money(ticketTypeRequest.price()));
                current.setCapacity(ticketTypeRequest.capacity());
                current.setAvailable(TicketBusinessRules.calculateAvailable(current.getCapacity(), current.getSold()));
            }
        }

        TicketEvent saved = ticketEventRepository.save(event);
        audit(saved.getOwnerId(), saved.getId(), null, saved.getOwnerId(), "EVENT_UPDATED", TicketAuditLevel.INFO, "Ticket event updated: " + saved.getName());
        return toEventResponse(saved);
    }

    public TicketPurchaseResponse purchaseTickets(PurchaseTicketsRequest request) {
        TicketEvent event = findEvent(request.eventId());
        if (event.getStatus() != TicketEventStatus.PUBLISHED) {
            throw new IllegalStateException("Event is not published for ticket sales.");
        }

        TicketType requestedTicketType = canonicalTicketType(request.ticketType());
        EventTicketType ticketType = eventTicketTypeRepository.findLockedByEventIdAndType(event.getId(), requestedTicketType)
                .orElseThrow(() -> new IllegalArgumentException("Ticket type not found for event."));
        TicketBusinessRules.requirePurchaseCapacity(ticketType.getCapacity(), ticketType.getSold(), request.quantity());

        BigDecimal subtotal = TicketBusinessRules.money(ticketType.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        BigDecimal checkoutFee = TicketBusinessRules.calculatePercentAmount(subtotal, ticketProperties.checkoutServiceFeePercent());
        BigDecimal total = TicketBusinessRules.money(subtotal.add(checkoutFee));

        TicketPayment payment = new TicketPayment();
        payment.setId(newId("ticket-payment"));
        payment.setEventId(event.getId());
        payment.setUserId(request.userId());
        payment.setBuyerName(request.buyerName().trim());
        payment.setBuyerEmail(request.buyerEmail().trim().toLowerCase());
        payment.setTicketType(requestedTicketType);
        payment.setQuantity(request.quantity());
        payment.setSubtotalAmount(subtotal);
        payment.setCheckoutServiceFeeAmount(checkoutFee);
        payment.setTotalAmount(total);
        payment.setStatus(TicketPaymentStatus.PENDING);
        payment.setPaymentProvider(STRIPE_PROVIDER);
        payment.setPaymentReference("PENDING");
        TicketPayment savedPayment = ticketPaymentRepository.save(payment);
        CreatedTicketPaymentLink paymentLink = stripeService.createTicketPaymentLink(savedPayment, event, request.callbackUrl());
        savedPayment.setPaymentReference(paymentLink.stripeId());
        savedPayment = ticketPaymentRepository.save(savedPayment);

        List<UserTicket> createdTickets = new ArrayList<>();
        for (int index = 0; index < request.quantity(); index++) {
            UserTicket ticket = new UserTicket();
            ticket.setId(newId("ticket"));
            ticket.setEventId(event.getId());
            ticket.setUserId(request.userId());
            ticket.setBuyerName(request.buyerName().trim());
            ticket.setBuyerEmail(request.buyerEmail().trim().toLowerCase());
            ticket.setTicketType(requestedTicketType);
            ticket.setPricePaid(TicketBusinessRules.money(total.divide(BigDecimal.valueOf(request.quantity()), 2, java.math.RoundingMode.HALF_UP)));
            ticket.setTicketReference(nextReference(event.getId()));
            ticket.setStatus(UserTicketStatus.ACTIVE);
            ticket.setQrCodeValue(qrPayload(ticket.getId(), event.getId(), ticket.getTicketReference(), request.userId()));
            createdTickets.add(userTicketRepository.save(ticket));
        }

        ticketType.setSold(ticketType.getSold() + request.quantity());
        ticketType.setAvailable(TicketBusinessRules.calculateAvailable(ticketType.getCapacity(), ticketType.getSold()));
        eventTicketTypeRepository.save(ticketType);

        audit(event.getOwnerId(), event.getId(), null, request.userId(), "TICKETS_CHECKOUT_CREATED", TicketAuditLevel.INFO,
                request.quantity() + " " + requestedTicketType + " ticket checkout created for " + event.getName());
        log.info("ticket_checkout_created eventId={} userId={} ticketType={} quantity={} total={} stripePaymentLink={}", event.getId(), request.userId(), requestedTicketType, request.quantity(), total, paymentLink.stripeId());

        return new TicketPurchaseResponse(toPaymentResponse(savedPayment, paymentLink.paymentUrl(), paymentLink.qrCodeUrl()), createdTickets.stream().map(this::toUserTicketResponse).toList());
    }

    public TicketEventPromotionResponse promoteEvent(String eventId, PromoteTicketEventRequest request, String actorUsername) {
        TicketEvent event = findEvent(eventId);
        Business business = trackerUserService.businessForActor(actorUsername);
        requireOwnerControlsEvent(event, business);

        BigDecimal amount = request.amount() == null ? ticketProperties.promotionPriceZar() : request.amount();
        BusinessAccountLedgerEntry ledgerEntry = businessAccountService.debitPromotion(
                business,
                amount,
                BusinessAccountEntryType.TICKET_PROMOTION_DEBIT,
                "Ticket event boost: " + event.getName(),
                actorUsername);

        TicketEventPromotion promotion = new TicketEventPromotion();
        promotion.setId(newId("ticket-event-boost"));
        promotion.setEventId(event.getId());
        promotion.setOwnerId(event.getOwnerId());
        promotion.setAmount(TicketBusinessRules.money(amount));
        promotion.setCurrency("ZAR");
        promotion.setStatus(TicketPromotionStatus.ACTIVE);
        promotion.setStartsAt(request.startsAt() == null ? Instant.now() : request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setBusinessAccountEntryId(ledgerEntry.getId());
        TicketEventPromotion saved = ticketEventBoostRepository.save(promotion);
        audit(event.getOwnerId(), event.getId(), null, actorUsername, "TICKET_EVENT_BOOSTED", TicketAuditLevel.INFO, "Ticket event boosted from business account: " + amount);
        return toPromotionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketEventPromotionResponse> getPromotionsForEvent(String eventId) {
        return ticketEventBoostRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream().map(this::toPromotionResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketEventPromotionResponse> getOwnerEventPromotions(String ownerId) {
        return ticketEventBoostRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::toPromotionResponse).toList();
    }

    public TicketEventPromotionResponse cancelPromotion(String promotionId, String actorUsername) {
        TicketEventPromotion promotion = ticketEventBoostRepository.findById(promotionId).orElseThrow(() -> new IllegalArgumentException("Ticket promotion not found."));
        TicketEvent event = findEvent(promotion.getEventId());
        Business business = trackerUserService.businessForActor(actorUsername);
        requireOwnerControlsEvent(event, business);
        promotion.setStatus(TicketPromotionStatus.CANCELLED);
        TicketEventPromotion saved = ticketEventBoostRepository.save(promotion);
        audit(event.getOwnerId(), event.getId(), null, actorUsername, "TICKET_EVENT_BOOST_CANCELLED", TicketAuditLevel.WARN, "Ticket event boost cancelled: " + promotionId);
        return toPromotionResponse(saved);
    }

    public TicketVerificationResponse verifyByQr(String qrCodeValue, String workerId) {
        UserTicket ticket = userTicketRepository.findByQrCodeValue(qrCodeValue).orElseThrow(() -> new IllegalArgumentException("Ticket QR code not found."));
        return verifyTicket(ticket, workerId, "QR ticket accepted.");
    }

    public TicketVerificationResponse verifyByReference(String ticketReference, String workerId) {
        UserTicket ticket = userTicketRepository.findByTicketReferenceIgnoreCase(ticketReference).orElseThrow(() -> new IllegalArgumentException("Ticket reference not found."));
        return verifyTicket(ticket, workerId, "Ticket reference accepted.");
    }

    private TicketVerificationResponse verifyTicket(UserTicket ticket, String workerId, String message) {
        if (ticket.getStatus() == UserTicketStatus.USED) {
            return new TicketVerificationResponse(false, "Ticket has already been used.", toUserTicketResponse(ticket), toEventResponse(findEvent(ticket.getEventId())));
        }

        if (ticket.getStatus() != UserTicketStatus.ACTIVE) {
            return new TicketVerificationResponse(false, "Ticket is not active.", toUserTicketResponse(ticket), toEventResponse(findEvent(ticket.getEventId())));
        }

        TicketEvent event = findEvent(ticket.getEventId());
        if (event.getStatus() != TicketEventStatus.PUBLISHED) {
            return new TicketVerificationResponse(false, "Event is not open for ticket verification.", toUserTicketResponse(ticket), toEventResponse(event));
        }

        ticket.setStatus(UserTicketStatus.USED);
        ticket.setUsedAt(Instant.now());
        ticket.setScannedByWorkerId(workerId);
        UserTicket saved = userTicketRepository.save(ticket);
        audit(event.getOwnerId(), event.getId(), saved.getId(), workerId, "TICKET_USED", TicketAuditLevel.INFO, "Ticket accepted and marked used.");
        return new TicketVerificationResponse(true, message, toUserTicketResponse(saved), toEventResponse(event));
    }

    @Transactional(readOnly = true)
    public OwnerTicketDashboardResponse getOwnerDashboard(String ownerId) {
        List<TicketEvent> ownerEvents = ticketEventRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        int totalCapacity = ownerEvents.stream().flatMap(event -> event.getTicketTypes().stream()).mapToInt(EventTicketType::getCapacity).sum();
        int totalSold = ownerEvents.stream().flatMap(event -> event.getTicketTypes().stream()).mapToInt(EventTicketType::getSold).sum();
        int totalAvailable = ownerEvents.stream().flatMap(event -> event.getTicketTypes().stream()).mapToInt(EventTicketType::getAvailable).sum();
        long regularSold = sumSold(ownerEvents, TicketType.REGULAR);
        long vipSold = sumSold(ownerEvents, TicketType.VIP);
        long vvipSold = sumSold(ownerEvents, TicketType.VVIP);
        List<String> eventIds = ownerEvents.stream().map(TicketEvent::getId).toList();
        BigDecimal revenue = eventIds.isEmpty() ? BigDecimal.ZERO : ticketPaymentRepository.sumSuccessfulPaymentsByEventIds(eventIds, TicketPaymentStatus.SUCCESS);
        BigDecimal withdrawn = ticketWithdrawalRepository.sumGrossByOwnerIdAndStatuses(ownerId, List.of(TicketWithdrawalStatus.REQUESTED, TicketWithdrawalStatus.APPROVED, TicketWithdrawalStatus.PAID));
        BigDecimal availableWithdrawalBalance = revenue.subtract(withdrawn).max(BigDecimal.ZERO);

        return new OwnerTicketDashboardResponse(
                ownerEvents.size(),
                ticketEventRepository.countByOwnerIdAndStatusAndEventDateGreaterThanEqual(ownerId, TicketEventStatus.PUBLISHED, LocalDate.now()),
                totalCapacity,
                totalSold,
                totalAvailable,
                regularSold,
                vipSold,
                vvipSold,
                TicketBusinessRules.money(revenue),
                TicketBusinessRules.money(availableWithdrawalBalance),
                ticketProperties.withdrawalFeePercent()
        );
    }

    @Transactional(readOnly = true)
    public List<TicketEventResponse> getOwnerEvents(String ownerId) {
        return ticketEventRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(this::toEventResponse).toList();
    }

    public TicketWithdrawalResponse requestWithdrawal(TicketWithdrawalRequest request) {
        OwnerTicketDashboardResponse dashboard = getOwnerDashboard(request.ownerId());
        if (request.grossAmount().compareTo(ticketProperties.withdrawalMinimumZar()) < 0) {
            throw new IllegalArgumentException("Ticket withdrawal is below the configured minimum amount.");
        }
        if (request.grossAmount().compareTo(dashboard.availableWithdrawalBalance()) > 0) {
            throw new IllegalStateException("Ticket withdrawal amount is greater than the available ticket balance.");
        }

        BigDecimal gross = TicketBusinessRules.money(request.grossAmount());
        BigDecimal fee = TicketBusinessRules.calculatePercentAmount(gross, ticketProperties.withdrawalFeePercent());
        TicketWithdrawal withdrawal = new TicketWithdrawal();
        withdrawal.setId(newId("ticket-withdrawal"));
        withdrawal.setOwnerId(request.ownerId());
        withdrawal.setGrossAmount(gross);
        withdrawal.setServiceFeePercent(ticketProperties.withdrawalFeePercent());
        withdrawal.setServiceFeeAmount(fee);
        withdrawal.setNetAmount(TicketBusinessRules.money(gross.subtract(fee)));
        withdrawal.setStatus(TicketWithdrawalStatus.REQUESTED);
        withdrawal.setNotes(blankToNull(request.notes()));
        TicketWithdrawal saved = ticketWithdrawalRepository.save(withdrawal);
        audit(request.ownerId(), null, null, request.ownerId(), "TICKET_WITHDRAWAL_REQUESTED", TicketAuditLevel.INFO,
                "Ticket withdrawal requested with 5% service fee: gross " + gross + ", net " + saved.getNetAmount());
        return toWithdrawalResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketWithdrawalResponse> getWithdrawals(String ownerId) {
        return ticketWithdrawalRepository.findByOwnerIdOrderByRequestedAtDesc(ownerId).stream().map(this::toWithdrawalResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAuditLogResponse> getLogs(Optional<String> ownerId, Optional<String> eventId) {
        if (eventId.isPresent()) return ticketAuditLogRepository.findTop100ByEventIdOrderByCreatedAtDesc(eventId.get()).stream().map(this::toLogResponse).toList();
        if (ownerId.isPresent()) return ticketAuditLogRepository.findTop100ByOwnerIdOrderByCreatedAtDesc(ownerId.get()).stream().map(this::toLogResponse).toList();
        return ticketAuditLogRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toLogResponse).toList();
    }

    private void requireTicketTypes(List<EventTicketTypeRequest> requestedTicketTypes) {
        EnumSet<TicketType> requiredTypes = EnumSet.of(TicketType.REGULAR, TicketType.VIP, TicketType.VVIP);
        requestedTicketTypes.forEach(ticketType -> requiredTypes.remove(canonicalTicketType(ticketType.type())));
        if (!requiredTypes.isEmpty()) {
            throw new IllegalArgumentException("Regular, VIP, and VVIP ticket types are required.");
        }
    }

    private void requireOwnerControlsEvent(TicketEvent event, Business business) {
        String businessOwnerId = business.getOwner() == null || business.getOwner().getId() == null ? null : String.valueOf(business.getOwner().getId());
        String businessOwnerUsername = business.getOwner() == null ? null : business.getOwner().getUsername();
        if (!event.getOwnerId().equals(businessOwnerId) && !event.getOwnerId().equals(businessOwnerUsername)) {
            throw new IllegalArgumentException("Ticket event does not belong to this business owner");
        }
    }

    private EventTicketType toTicketTypeEntity(String eventId, EventTicketTypeRequest request) {
        TicketType ticketType = canonicalTicketType(request.type());
        EventTicketType entity = new EventTicketType();
        entity.setId(eventId + "-" + ticketType.name().toLowerCase());
        entity.setType(ticketType);
        entity.setPrice(TicketBusinessRules.money(request.price()));
        entity.setCapacity(request.capacity());
        entity.setSold(0);
        entity.setAvailable(TicketBusinessRules.calculateAvailable(request.capacity(), 0));
        return entity;
    }

    private TicketType canonicalTicketType(String ticketType) {
        return TicketType.fromInput(ticketType);
    }

    private TicketEvent findEvent(String eventId) {
        return ticketEventRepository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Ticket event not found."));
    }

    private long sumSold(List<TicketEvent> events, TicketType type) {
        return events.stream().flatMap(event -> event.getTicketTypes().stream()).filter(ticketType -> ticketType.getType() == type).mapToLong(EventTicketType::getSold).sum();
    }

    private String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String nextReference(String eventId) {
        return "KST-" + eventId.replace("event-", "").substring(0, Math.min(8, eventId.length())).toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String qrPayload(String ticketId, String eventId, String reference, String userId) {
        return "KST-TICKET:ticketId=" + ticketId + ";eventId=" + eventId + ";ticketReference=" + reference + ";userId=" + userId;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> copyIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void audit(String ownerId, String eventId, String ticketId, String actorId, String action, TicketAuditLevel level, String message) {
        TicketAuditLog auditLog = new TicketAuditLog();
        auditLog.setId(newId("ticket-log"));
        auditLog.setOwnerId(ownerId);
        auditLog.setEventId(eventId);
        auditLog.setTicketId(ticketId);
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setLevel(level);
        auditLog.setMessage(message);
        auditLog.setStructuredMetadata("ownerId=" + ownerId + ",eventId=" + eventId + ",ticketId=" + ticketId + ",actorId=" + actorId + ",action=" + action);
        ticketAuditLogRepository.save(auditLog);
    }

    private TicketEventResponse toEventResponse(TicketEvent event) {
        return new TicketEventResponse(
                event.getId(),
                event.getOwnerId(),
                event.getName(),
                event.getDescription(),
                event.getLocation(),
                event.getEventDate(),
                event.getEventTime(),
                event.getBannerUrl(),
                event.getPosterPhotoUrl(),
                event.getStatus(),
                event.getTicketTypes().stream().map(this::toTicketTypeResponse).sorted(Comparator.comparing(EventTicketTypeResponse::type)).toList(),
                List.copyOf(event.getWorkerIds()),
                List.copyOf(event.getAffiliateIds()),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private EventTicketTypeResponse toTicketTypeResponse(EventTicketType ticketType) {
        return new EventTicketTypeResponse(ticketType.getId(), ticketType.getEvent().getId(), ticketType.getType(), ticketType.getPrice(), ticketType.getCapacity(), ticketType.getSold(), ticketType.getAvailable());
    }

    private UserTicketResponse toUserTicketResponse(UserTicket ticket) {
        return new UserTicketResponse(ticket.getId(), ticket.getEventId(), ticket.getUserId(), ticket.getBuyerName(), ticket.getBuyerEmail(), ticket.getTicketType(), ticket.getPricePaid(), ticket.getQrCodeValue(), ticket.getTicketReference(), ticket.getStatus(), ticket.getPurchasedAt(), ticket.getUsedAt(), ticket.getScannedByWorkerId());
    }

    private TicketPaymentResponse toPaymentResponse(TicketPayment payment) {
        return toPaymentResponse(payment, null, null);
    }

    private TicketPaymentResponse toPaymentResponse(TicketPayment payment, String paymentUrl, String qrCodeUrl) {
        return new TicketPaymentResponse(payment.getId(), payment.getEventId(), payment.getUserId(), payment.getTicketType(), payment.getQuantity(), payment.getSubtotalAmount(), payment.getCheckoutServiceFeeAmount(), payment.getTotalAmount(), payment.getStatus(), payment.getPaymentProvider(), payment.getPaymentReference(), paymentUrl, qrCodeUrl, payment.getCreatedAt());
    }

    private TicketWithdrawalResponse toWithdrawalResponse(TicketWithdrawal withdrawal) {
        return new TicketWithdrawalResponse(withdrawal.getId(), withdrawal.getOwnerId(), withdrawal.getGrossAmount(), withdrawal.getServiceFeePercent(), withdrawal.getServiceFeeAmount(), withdrawal.getNetAmount(), withdrawal.getStatus(), withdrawal.getRequestedAt(), withdrawal.getProcessedAt(), withdrawal.getNotes());
    }

    private TicketEventPromotionResponse toPromotionResponse(TicketEventPromotion promotion) {
        return new TicketEventPromotionResponse(promotion.getId(), promotion.getEventId(), promotion.getOwnerId(), promotion.getAmount(), promotion.getCurrency(), promotion.getStatus(), promotion.getStartsAt(), promotion.getEndsAt(), promotion.getBusinessAccountEntryId(), promotion.getCreatedAt(), promotion.getUpdatedAt());
    }

    private TicketAuditLogResponse toLogResponse(TicketAuditLog log) {
        return new TicketAuditLogResponse(log.getId(), log.getOwnerId(), log.getEventId(), log.getTicketId(), log.getActorId(), log.getAction(), log.getLevel(), log.getMessage(), log.getStructuredMetadata(), log.getCreatedAt());
    }
}
