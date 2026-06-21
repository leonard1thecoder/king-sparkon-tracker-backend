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
@Table(name = "business_subscriptions")
public class BusinessSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_plan", nullable = false, length = 32)
	private BusinessPlan businessPlan;

	@Enumerated(EnumType.STRING)
	@Column(name = "billing_interval", nullable = false, length = 32)
	private BillingInterval billingInterval;

	@Column(name = "term_years")
	private Integer termYears;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SubscriptionPaymentStatus status = SubscriptionPaymentStatus.CREATED;

	@Column(name = "paypal_subscription_id")
	private String paypalSubscriptionId;

	@Column(name = "paypal_subscription_token")
	private String paypalSubscriptionToken;

	@Column(name = "paypal_plan_id")
	private String paypalPlanId;

	@Column(name = "paypal_approval_url", length = 2048)
	private String paypalApprovalUrl;

	@Column(name = "period_start_date")
	private LocalDateTime periodStartDate;

	@Column(name = "period_end_date")
	private LocalDateTime periodEndDate;

	@Column(name = "created_date", nullable = false)
	private LocalDateTime createdDate;

	@Column(name = "modified_date", nullable = false)
	private LocalDateTime modifiedDate;

	protected BusinessSubscription() {
	}

	public BusinessSubscription(
			Business business,
			BusinessPlan businessPlan,
			BillingInterval billingInterval,
			Integer termYears,
			BigDecimal amount,
			String currency) {
		this.business = business;
		this.businessPlan = businessPlan;
		this.billingInterval = billingInterval;
		this.termYears = termYears;
		this.amount = amount;
		this.currency = currency;
		this.status = SubscriptionPaymentStatus.CREATED;
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();

		if (createdDate == null) {
			createdDate = now;
		}

		if (status == null) {
			status = SubscriptionPaymentStatus.CREATED;
		}

		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		modifiedDate = LocalDateTime.now();
	}

	public void markApprovalPending(String paypalSubscriptionId, String paypalSubscriptionToken, String paypalPlanId, String paypalApprovalUrl) {
		this.paypalSubscriptionId = paypalSubscriptionId;
		this.paypalSubscriptionToken = paypalSubscriptionToken;
		this.paypalPlanId = paypalPlanId;
		this.paypalApprovalUrl = paypalApprovalUrl;
		this.status = SubscriptionPaymentStatus.APPROVAL_PENDING;
	}

	public void activate(LocalDateTime periodStartDate, LocalDateTime periodEndDate) {
		this.periodStartDate = periodStartDate;
		this.periodEndDate = periodEndDate;
		this.status = SubscriptionPaymentStatus.ACTIVE;
	}

	public void markPaymentFailed() {
		this.status = SubscriptionPaymentStatus.PAYMENT_FAILED;
	}

	public void expire() {
		this.status = SubscriptionPaymentStatus.EXPIRED;
	}

	public void cancel() {
		this.status = SubscriptionPaymentStatus.CANCELLED;
	}

	public Long getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public BusinessPlan getBusinessPlan() {
		return businessPlan;
	}

	public BillingInterval getBillingInterval() {
		return billingInterval;
	}

	public Integer getTermYears() {
		return termYears;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public SubscriptionPaymentStatus getStatus() {
		return status;
	}

	public String getPaypalSubscriptionId() {
		return paypalSubscriptionId;
	}

	public String getPaypalSubscriptionToken() {
		return paypalSubscriptionToken;
	}

	public String getPaypalPlanId() {
		return paypalPlanId;
	}

	public String getPaypalApprovalUrl() {
		return paypalApprovalUrl;
	}

	public LocalDateTime getPeriodStartDate() {
		return periodStartDate;
	}

	public LocalDateTime getPeriodEndDate() {
		return periodEndDate;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}
}
