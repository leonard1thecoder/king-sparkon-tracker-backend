package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_withdrawals")
public class TransactionWithdrawal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(name = "business_id", nullable = false)
	private Long businessId;

	@Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal grossAmount;

	@Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal feeAmount;

	@Column(name = "fee_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal feePercent;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "transaction_count", nullable = false)
	private int transactionCount;

	@Column(name = "paypal_email", nullable = false)
	private String paypalEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TransactionWithdrawalStatus status = TransactionWithdrawalStatus.REQUESTED;

	@Column(name = "requested_at", nullable = false)
	private OffsetDateTime requestedAt;

	@Column(nullable = false)
	private OffsetDateTime updated;

	protected TransactionWithdrawal() {
	}

	public TransactionWithdrawal(
			Long ownerId,
			Long businessId,
			BigDecimal grossAmount,
			BigDecimal feeAmount,
			BigDecimal feePercent,
			BigDecimal amount,
			String currency,
			int transactionCount,
			String paypalEmail) {
		OffsetDateTime now = OffsetDateTime.now();
		this.ownerId = ownerId;
		this.businessId = businessId;
		this.grossAmount = grossAmount;
		this.feeAmount = feeAmount;
		this.feePercent = feePercent;
		this.amount = amount;
		this.currency = currency;
		this.transactionCount = transactionCount;
		this.paypalEmail = paypalEmail;
		this.status = TransactionWithdrawalStatus.REQUESTED;
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
			status = TransactionWithdrawalStatus.REQUESTED;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updated = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public Long getBusinessId() {
		return businessId;
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public BigDecimal getFeeAmount() {
		return feeAmount;
	}

	public BigDecimal getFeePercent() {
		return feePercent;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public int getTransactionCount() {
		return transactionCount;
	}

	public String getPaypalEmail() {
		return paypalEmail;
	}

	public TransactionWithdrawalStatus getStatus() {
		return status;
	}

	public OffsetDateTime getRequestedAt() {
		return requestedAt;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}
}
