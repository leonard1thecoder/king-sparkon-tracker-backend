package com.king_sparkon_tracker.backend.tickets.reservation;

import java.time.Instant;
import java.util.UUID;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "ticket_reservations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_ticket_reservation_payment",
				columnNames = "payment_id"))
public class TicketReservation {
	@Id
	@Column(length = 64)
	private String id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "business_id")
	private Business business;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private TicketEvent event;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_type_id", nullable = false)
	private EventTicketType ticketType;
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private TicketPayment payment;
	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;
	@Column(nullable = false)
	private int quantity;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private TicketReservationStatus status;
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
	@Version
	private long version;

	protected TicketReservation() {
	}
	public TicketReservation(
			Business business,
			TicketEvent event,
			EventTicketType ticketType,
			TicketPayment payment,
			String userId,
			int quantity,
			Instant expiresAt) {
		this.id = "TKR-" + UUID.randomUUID();
		this.business = business;
		this.event = event;
		this.ticketType = ticketType;
		this.payment = payment;
		this.userId = userId;
		this.quantity = quantity;
		this.expiresAt = expiresAt;
		this.status = TicketReservationStatus.ACTIVE;
	}
	@PrePersist void beforeCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
	public void consume() {
		if (status == TicketReservationStatus.CONSUMED) return;
		if (status != TicketReservationStatus.ACTIVE) throw new IllegalStateException("Only ACTIVE ticket reservations can be consumed");
		status = TicketReservationStatus.CONSUMED;
	}
	public void release(boolean expired) {
		if (status != TicketReservationStatus.ACTIVE) return;
		status = expired ? TicketReservationStatus.EXPIRED : TicketReservationStatus.RELEASED;
	}
	public String getId() { return id; }
	public Business getBusiness() { return business; }
	public TicketEvent getEvent() { return event; }
	public EventTicketType getTicketType() { return ticketType; }
	public TicketPayment getPayment() { return payment; }
	public String getUserId() { return userId; }
	public int getQuantity() { return quantity; }
	public TicketReservationStatus getStatus() { return status; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
