package com.king_sparkon_tracker.backend.tickets.reservation;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.outbox.OutboxEventType;
import com.king_sparkon_tracker.backend.outbox.OutboxPayloads;
import com.king_sparkon_tracker.backend.outbox.OutboxPublisher;
import com.king_sparkon_tracker.backend.service.BusinessAccountService;
import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.UserTicket;
import com.king_sparkon_tracker.backend.tickets.model.UserTicketStatus;
import com.king_sparkon_tracker.backend.tickets.repository.EventTicketTypeRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketPaymentRepository;
import com.king_sparkon_tracker.backend.tickets.repository.UserTicketRepository;

@Service
@Transactional
public class TicketReservationService {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

	private final TicketReservationRepository reservationRepository;
	private final EventTicketTypeRepository ticketTypeRepository;
	private final TicketPaymentRepository paymentRepository;
	private final UserTicketRepository userTicketRepository;
	private final BusinessAccountService businessAccountService;
	private final OutboxPublisher outboxPublisher;

	public TicketReservationService(
			TicketReservationRepository reservationRepository,
			EventTicketTypeRepository ticketTypeRepository,
			TicketPaymentRepository paymentRepository,
			UserTicketRepository userTicketRepository,
			BusinessAccountService businessAccountService,
			OutboxPublisher outboxPublisher) {
		this.reservationRepository = reservationRepository;
		this.ticketTypeRepository = ticketTypeRepository;
		this.paymentRepository = paymentRepository;
		this.userTicketRepository = userTicketRepository;
		this.businessAccountService = businessAccountService;
		this.outboxPublisher = outboxPublisher;
	}

	public TicketReservation reserve(
			Business business,
			EventTicketType ticketType,
			TicketPayment payment,
			String userId,
			int quantity) {
		TicketReservation existing = reservationRepository.findByPayment_Id(payment.getId()).orElse(null);
		if (existing != null) return existing;
		ticketType.reserve(quantity);
		ticketTypeRepository.save(ticketType);
		return reservationRepository.save(new TicketReservation(
				business,
				ticketType.getEvent(),
				ticketType,
				payment,
				userId,
				quantity,
				Instant.now().plus(DEFAULT_TTL)));
	}

	public TicketPayment confirm(String paymentId, String providerReference) {
		TicketReservation reservation = reservationRepository.findLockedByPaymentId(paymentId)
				.orElseThrow(() -> new IllegalStateException("Ticket payment has no active reservation: " + paymentId));
		TicketPayment payment = paymentRepository.findLockedById(paymentId)
				.orElseThrow(() -> new IllegalStateException("Ticket payment not found: " + paymentId));
		if (payment.getStatus() == TicketPaymentStatus.SUCCESS && reservation.getStatus() == TicketReservationStatus.CONSUMED) {
			return payment;
		}
		EventTicketType ticketType = ticketTypeRepository.findLockedByEventIdAndType(
				reservation.getEvent().getId(), reservation.getTicketType().getType())
				.orElseThrow(() -> new IllegalStateException("Reserved ticket type no longer exists"));
		ticketType.consumeReservation(reservation.getQuantity());
		ticketTypeRepository.save(ticketType);
		payment.setStatus(TicketPaymentStatus.SUCCESS);
		if (providerReference != null && !providerReference.isBlank()) payment.setPaymentReference(providerReference);
		paymentRepository.save(payment);

		for (UserTicket ticket : userTicketRepository.findByPaymentIdOrderByPurchasedAtAsc(paymentId)) {
			if (ticket.getStatus() != UserTicketStatus.PENDING_PAYMENT) continue;
			ticket.setStatus(UserTicketStatus.ACTIVE);
			String qrValue = qrPayload(ticket);
			ticket.setQrCodeValue(qrValue);
			userTicketRepository.save(ticket);
			outboxPublisher.publish(
					"USER_TICKET",
					ticket.getId(),
					OutboxEventType.QR_GENERATION,
					new OutboxPayloads.QrGeneration("USER_TICKET", ticket.getId(), qrValue),
					"ticket-qr:" + ticket.getId() + ":" + ticket.getOwnershipVersion());
		}
		reservation.consume();
		reservationRepository.save(reservation);
		if (reservation.getBusiness() != null) {
			businessAccountService.postRevenueCreditIfAbsent(
					reservation.getBusiness(),
					payment.getSubtotalAmount().setScale(2, RoundingMode.HALF_UP),
					BusinessAccountEntryType.TICKET_SALE_CREDIT,
					"TICKET-PAYMENT:" + payment.getId(),
					"Successful ticket payment " + payment.getId());
		}
		return payment;
	}

	public void release(String paymentId, boolean expired) {
		TicketReservation reservation = reservationRepository.findLockedByPaymentId(paymentId).orElse(null);
		if (reservation == null || reservation.getStatus() != TicketReservationStatus.ACTIVE) return;
		EventTicketType ticketType = ticketTypeRepository.findLockedByEventIdAndType(
				reservation.getEvent().getId(), reservation.getTicketType().getType())
				.orElseThrow(() -> new IllegalStateException("Reserved ticket type no longer exists"));
		ticketType.releaseReservation(reservation.getQuantity());
		ticketTypeRepository.save(ticketType);
		TicketPayment payment = paymentRepository.findLockedById(paymentId).orElse(null);
		if (payment != null && payment.getStatus() == TicketPaymentStatus.PENDING) {
			payment.setStatus(TicketPaymentStatus.FAILED);
			paymentRepository.save(payment);
		}
		userTicketRepository.findByPaymentIdOrderByPurchasedAtAsc(paymentId).forEach(ticket -> {
			if (ticket.getStatus() == UserTicketStatus.PENDING_PAYMENT) {
				ticket.setStatus(expired ? UserTicketStatus.EXPIRED : UserTicketStatus.CANCELLED);
				userTicketRepository.save(ticket);
			}
		});
		reservation.release(expired);
		reservationRepository.save(reservation);
	}

	@Scheduled(fixedDelayString = "${app.ticket-reservations.expiry-delay-ms:60000}")
	public void expireReservations() {
		reservationRepository.findExpiredLocked(List.of(TicketReservationStatus.ACTIVE), Instant.now())
				.forEach(reservation -> release(reservation.getPayment().getId(), true));
	}

	private String qrPayload(UserTicket ticket) {
		return "KST-TICKET:ticketId=" + ticket.getId()
				+ ";eventId=" + ticket.getEventId()
				+ ";ticketReference=" + ticket.getTicketReference()
				+ ";userId=" + ticket.getUserId()
				+ ";ownershipVersion=" + ticket.getOwnershipVersion();
	}
}
