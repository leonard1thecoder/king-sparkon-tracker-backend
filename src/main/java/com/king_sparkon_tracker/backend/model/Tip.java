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
@Table(name = "tips")
public class Tip {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "worker_id", nullable = false)
	private TrackerUser worker;

	@Column(name = "tip_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal tipAmount;

	@Column(nullable = false)
	private OffsetDateTime created;

	@Column(nullable = false)
	private OffsetDateTime updated;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TipStatus status = TipStatus.UNPAID;

	@Column(name = "payment_reference")
	private String paymentReference;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "withdrawal_id")
	private TipWithdrawal withdrawal;

	protected Tip() {
	}

	public Tip(TrackerUser worker, BigDecimal tipAmount) {
		OffsetDateTime now = OffsetDateTime.now();
		this.worker = worker;
		this.tipAmount = tipAmount;
		this.created = now;
		this.updated = now;
		this.status = TipStatus.UNPAID;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (created == null) {
			created = now;
		}
		if (updated == null) {
			updated = now;
		}
		if (status == null) {
			status = TipStatus.UNPAID;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updated = OffsetDateTime.now();
	}

	public void markPaymentReference(String paymentReference) {
		this.paymentReference = paymentReference;
		this.updated = OffsetDateTime.now();
	}

	public void markPaid() {
		if (status != TipStatus.UNPAID) {
			throw new IllegalArgumentException("Only UNPAID tips can be marked as PAID");
		}
		this.status = TipStatus.PAID;
		this.updated = OffsetDateTime.now();
	}

	public void assignWithdrawal(TipWithdrawal withdrawal) {
		this.withdrawal = withdrawal;
		this.updated = OffsetDateTime.now();
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

	public BigDecimal getTipAmount() {
		return tipAmount;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public TipStatus getStatus() {
		return status;
	}

	public String getPaymentReference() {
		return paymentReference;
	}

	public Long getWithdrawalId() {
		return withdrawal == null ? null : withdrawal.getId();
	}

	public TipWithdrawal getWithdrawal() {
		return withdrawal;
	}
}
