package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "affiliate_withdrawals")
public class AffiliateWithdrawal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "affiliate_user_id", nullable = false)
	private TrackerUser affiliate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "commission_count", nullable = false)
	private int commissionCount;

	@Column(name = "paypal_link", nullable = false, length = 2048)
	private String paypalLink;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AffiliateWithdrawalStatus status = AffiliateWithdrawalStatus.REQUESTED;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(nullable = false)
	private LocalDateTime updated;

	protected AffiliateWithdrawal() {
	}

	public AffiliateWithdrawal(
			TrackerUser affiliate,
			BigDecimal amount,
			String currency,
			int commissionCount,
			String paypalLink) {
		LocalDateTime now = LocalDateTime.now();
		this.affiliate = affiliate;
		this.amount = amount;
		this.currency = currency;
		this.commissionCount = commissionCount;
		this.paypalLink = paypalLink;
		this.status = AffiliateWithdrawalStatus.REQUESTED;
		this.requestedAt = now;
		this.updated = now;
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (requestedAt == null) {
			requestedAt = now;
		}
		if (updated == null) {
			updated = now;
		}
		if (status == null) {
			status = AffiliateWithdrawalStatus.REQUESTED;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updated = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getAffiliateId() {
		return affiliate == null ? null : affiliate.getId();
	}

	public TrackerUser getAffiliate() {
		return affiliate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public int getCommissionCount() {
		return commissionCount;
	}

	public String getPaypalLink() {
		return paypalLink;
	}

	public AffiliateWithdrawalStatus getStatus() {
		return status;
	}

	public LocalDateTime getRequestedAt() {
		return requestedAt;
	}

	public LocalDateTime getUpdated() {
		return updated;
	}
}
