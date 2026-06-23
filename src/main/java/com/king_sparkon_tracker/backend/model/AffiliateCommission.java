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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "affiliate_commissions", uniqueConstraints = {
		@UniqueConstraint(name = "uk_affiliate_commissions_subscription", columnNames = "subscription_id")
})
public class AffiliateCommission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "affiliate_user_id", nullable = false)
	private TrackerUser affiliate;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "subscription_id", nullable = false)
	private BusinessSubscription subscription;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "withdrawal_id")
	private AffiliateWithdrawal withdrawal;

	@Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal grossAmount;

	@Column(name = "commission_rate_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal commissionRatePercent;

	@Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal commissionAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AffiliateCommissionStatus status = AffiliateCommissionStatus.EARNED;

	@Column(name = "earned_at", nullable = false)
	private LocalDateTime earnedAt;

	@Column(nullable = false)
	private LocalDateTime updated;

	protected AffiliateCommission() {
	}

	public AffiliateCommission(
			TrackerUser affiliate,
			Business business,
			BusinessSubscription subscription,
			BigDecimal grossAmount,
			BigDecimal commissionRatePercent,
			BigDecimal commissionAmount,
			String currency,
			LocalDateTime earnedAt) {
		this.affiliate = affiliate;
		this.business = business;
		this.subscription = subscription;
		this.grossAmount = grossAmount;
		this.commissionRatePercent = commissionRatePercent;
		this.commissionAmount = commissionAmount;
		this.currency = currency;
		this.status = AffiliateCommissionStatus.EARNED;
		this.earnedAt = earnedAt == null ? LocalDateTime.now() : earnedAt;
		this.updated = this.earnedAt;
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (earnedAt == null) {
			earnedAt = now;
		}
		if (updated == null) {
			updated = now;
		}
		if (status == null) {
			status = AffiliateCommissionStatus.EARNED;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updated = LocalDateTime.now();
	}

	public void assignWithdrawal(AffiliateWithdrawal withdrawal) {
		this.withdrawal = withdrawal;
		this.status = AffiliateCommissionStatus.WITHDRAWAL_REQUESTED;
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

	public Long getBusinessId() {
		return business == null ? null : business.getId();
	}

	public Business getBusiness() {
		return business;
	}

	public Long getSubscriptionId() {
		return subscription == null ? null : subscription.getId();
	}

	public BusinessSubscription getSubscription() {
		return subscription;
	}

	public Long getWithdrawalId() {
		return withdrawal == null ? null : withdrawal.getId();
	}

	public BigDecimal getGrossAmount() {
		return grossAmount;
	}

	public BigDecimal getCommissionRatePercent() {
		return commissionRatePercent;
	}

	public BigDecimal getCommissionAmount() {
		return commissionAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public AffiliateCommissionStatus getStatus() {
		return status;
	}

	public LocalDateTime getEarnedAt() {
		return earnedAt;
	}

	public LocalDateTime getUpdated() {
		return updated;
	}
}
