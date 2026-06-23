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
@Table(name = "tip_withdrawals")
public class TipWithdrawal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "worker_id", nullable = false)
	private TrackerUser worker;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private TrackerUser owner;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "tip_count", nullable = false)
	private int tipCount;

	@Column(name = "paypal_email", nullable = false)
	private String paypalEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TipWithdrawalStatus status = TipWithdrawalStatus.REQUESTED;

	@Column(name = "requested_at", nullable = false)
	private OffsetDateTime requestedAt;

	@Column(nullable = false)
	private OffsetDateTime updated;

	protected TipWithdrawal() {
	}

	public TipWithdrawal(
			TrackerUser worker,
			TrackerUser owner,
			BigDecimal amount,
			String currency,
			int tipCount,
			String paypalEmail) {
		OffsetDateTime now = OffsetDateTime.now();
		this.worker = worker;
		this.owner = owner;
		this.amount = amount;
		this.currency = currency;
		this.tipCount = tipCount;
		this.paypalEmail = paypalEmail;
		this.status = TipWithdrawalStatus.REQUESTED;
		this.requestedAt = now;
		this.updated = now;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (requestedAt == null) {
			requestedAt = now;
		}
		if (updated == null) {
			updated = now;
		}
		if (status == null) {
			status = TipWithdrawalStatus.REQUESTED;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updated = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getWorkerId() {
		return worker == null ? null : worker.getId();
	}

	public TrackerUser getWorker() {
		return worker;
	}

	public Long getOwnerId() {
		return owner == null ? null : owner.getId();
	}

	public TrackerUser getOwner() {
		return owner;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public int getTipCount() {
		return tipCount;
	}

	public String getPaypalEmail() {
		return paypalEmail;
	}

	public TipWithdrawalStatus getStatus() {
		return status;
	}

	public OffsetDateTime getRequestedAt() {
		return requestedAt;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}
}
