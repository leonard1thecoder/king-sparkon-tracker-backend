package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "business_withdrawals")
public class BusinessWithdrawal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "payout_method", nullable = false, length = 40)
	private String payoutMethod;

	@Column(name = "payout_destination", length = 320)
	private String payoutDestination;

	@Column(length = 700)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private BusinessWithdrawalStatus status = BusinessWithdrawalStatus.REQUESTED;

	@Column(name = "ledger_entry_id")
	private Long ledgerEntryId;

	@Column(name = "requested_at", nullable = false)
	private OffsetDateTime requestedAt;

	@Column(name = "processed_at")
	private OffsetDateTime processedAt;

	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected BusinessWithdrawal() {
	}

	public BusinessWithdrawal(
			Business business,
			Long ownerId,
			BigDecimal amount,
			String payoutMethod,
			String payoutDestination,
			String notes) {
		this.business = business;
		this.ownerId = ownerId;
		this.amount = amount;
		this.payoutMethod = payoutMethod;
		this.payoutDestination = payoutDestination;
		this.notes = notes;
		this.status = BusinessWithdrawalStatus.REQUESTED;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (requestedAt == null) requestedAt = now;
		if (updatedAt == null) updatedAt = now;
		if (status == null) status = BusinessWithdrawalStatus.REQUESTED;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	public void attachLedgerEntry(Long ledgerEntryId) {
		this.ledgerEntryId = ledgerEntryId;
	}

	public Long getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getPayoutMethod() {
		return payoutMethod;
	}

	public String getPayoutDestination() {
		return payoutDestination;
	}

	public String getNotes() {
		return notes;
	}

	public BusinessWithdrawalStatus getStatus() {
		return status;
	}

	public Long getLedgerEntryId() {
		return ledgerEntryId;
	}

	public OffsetDateTime getRequestedAt() {
		return requestedAt;
	}

	public OffsetDateTime getProcessedAt() {
		return processedAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
