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
import com.king_sparkon_tracker.backend.model.TrackerUser;
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

	public EmbeddedTicketFulfilmentService(
			TicketEventRepository ticketEventRepository,
			EventTicketTypeRepository eventTicketTypeRepository,
			TicketPaymentRepository ticketPaymentRepository,
			UserTicketRepository userTicketRepository,
			TicketAuditLogRepository ticketAuditLogRepository,
			TicketProperties ticketProperties) {
		this.ticketEventRepository = ticketEventRepository;
		this.eventTicketTypeRepository = eventTicketTypeRepository;
		this.ticketPaymentRepository = ticketPaymentRepository;
		this.userTicketRepository = userTicketRepository;
		this.ticketAuditLogRepository = ticketAuditLogRepository;
		this.ticketProperties = ticketProperties;
	}

	public String fulfil(
			TicketItem item,
			TrackerUser actor,
			String buyerName,
			String buyerEmail,
			String paymentIntentId) {
		TicketEvent event = ticketEventRepository.findById(item.eventId())
				.orElseThrow(() -> new ResourceNotFoundException("Ticket event not found: " + item.eventId()));
		if (event.getStatus() != TicketEventStatus.PUBLISHED) {
			throw new IllegalStateException("Event is not published for ticket sales: " + event.getName());
		}

		EventTicketType ticketType = eventTicketTypeRepository.findLockedByEventIdAndType(event.getId(), item.ticketType())
				.orElseThrow(() -> new IllegalArgumentException("Ticket type is unavailable for event: " + event.getName()));
		TicketBusinessRules.requirePurchaseCapacity(ticketType.getCapacity(), ticketType.getSold(), item.quantity());

		BigDecimal subtotal = TicketBusinessRules.money(ticketType.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
		BigDecimal checkoutFee = TicketBusinessRules.calculatePercentAmount(subtotal, ticketProperties.checkoutServiceFeePercent());
		BigDecimal total = TicketBusinessRules.money(subtotal.add(checkoutFee));
		String actorId = String.valueOf(actor.getId());

		TicketPayment payment = new TicketPayment();
		payment.setId(newId("ticket-payment"));
		payment.setEventId(event.getId());
		payment.setUserId(actorId);
		payment.setBuyerName(buyerName.trim());
		payment.setBuyerEmail(buyerEmail.trim().toLowerCase());
		payment.setTicketType(item.ticketType());
		payment.setQuantity(item.quantity());
		payment.setSubtotalAmount(subtotal);
		payment.setCheckoutServiceFeeAmount(checkoutFee);
		payment.setTotalAmount(total);
		payment.setStatus(TicketPaymentStatus.SUCCESS);
		payment.setPaymentProvider(STRIPE_PROVIDER);
		payment.setPaymentReference(paymentIntentId);
		TicketPayment savedPayment = ticketPaymentRepository.save(payment);

		BigDecimal perTicket = TicketBusinessRules.money(total.divide(BigDecimal.valueOf(item.quantity()), 2, RoundingMode.HALF_UP));
		List<UserTicket> issuedTickets = new ArrayList<>();
		for (int index = 0; index < item.quantity(); index++) {
			UserTicket ticket = new UserTicket();
			ticket.setId(newId("ticket"));
			ticket.setEventId(event.getId());
			ticket.setUserId(actorId);
			ticket.setBuyerName(buyerName.trim());
			ticket.setBuyerEmail(buyerEmail.trim().toLowerCase());
			ticket.setTicketType(item.ticketType());
			ticket.setPricePaid(perTicket);
			ticket.setTicketReference(nextReference(event.getId()));
			ticket.setStatus(UserTicketStatus.ACTIVE);
			ticket.setQrCodeValue(qrPayload(ticket.getId(), event.getId(), ticket.getTicketReference(), actorId));
			issuedTickets.add(ticket);
		}
		userTicketRepository.saveAll(issuedTickets);

		ticketType.setSold(ticketType.getSold() + item.quantity());
		ticketType.setAvailable(TicketBusinessRules.calculateAvailable(ticketType.getCapacity(), ticketType.getSold()));
		eventTicketTypeRepository.save(ticketType);

		TicketAuditLog auditLog = new TicketAuditLog();
		auditLog.setId(newId("ticket-log"));
		auditLog.setOwnerId(event.getOwnerId());
		auditLog.setEventId(event.getId());
		auditLog.setActorId(actorId);
		auditLog.setAction("TICKETS_EMBEDDED_PAYMENT_FULFILLED");
		auditLog.setLevel(TicketAuditLevel.INFO);
		auditLog.setMessage(item.quantity() + " " + item.ticketType() + " tickets issued after Stripe PaymentIntent success");
		auditLog.setStructuredMetadata("ownerId=" + event.getOwnerId()
				+ ",eventId=" + event.getId()
				+ ",actorId=" + actorId
				+ ",paymentIntentId=" + paymentIntentId
				+ ",ticketPaymentId=" + savedPayment.getId());
		ticketAuditLogRepository.save(auditLog);

		return savedPayment.getId();
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

	private String qrPayload(String ticketId, String eventId, String reference, String userId) {
		return "KST-TICKET:ticketId=" + ticketId + ";eventId=" + eventId + ";ticketReference=" + reference + ";userId=" + userId;
	}
}
