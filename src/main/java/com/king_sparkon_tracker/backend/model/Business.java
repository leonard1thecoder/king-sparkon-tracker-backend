package com.king_sparkon_tracker.backend.model;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "businesses")
public class Business {

	private static final int FREE_TRIAL_DAYS = 14;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false, unique = true)
	private TrackerUser owner;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_plan", nullable = false, length = 32)
	private BusinessPlan businessPlan = BusinessPlan.FREE_TRIAL;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_status", nullable = false, length = 32)
	private BusinessStatus businessStatus = BusinessStatus.TRIAL;

	@Column(name = "trial_start_date")
	private LocalDateTime trialStartDate;

	@Column(name = "trial_end_date")
	private LocalDateTime trialEndDate;

	@Column(name = "subscription_start_date")
	private LocalDateTime subscriptionStartDate;

	@Column(name = "subscription_end_date")
	private LocalDateTime subscriptionEndDate;

	@Column(name = "current_billing_period_start_date")
	private LocalDateTime currentBillingPeriodStartDate;

	@Column(name = "current_billing_period_end_date")
	private LocalDateTime currentBillingPeriodEndDate;

	@Column(name = "paypal_subscription_id")
	private String paypalSubscriptionId;

	@Column(name = "paypal_subscription_token")
	private String paypalSubscriptionToken;

	@Column(name = "paypal_plan_id")
	private String paypalPlanId;

	@Column(name = "last_payment_date")
	private LocalDateTime lastPaymentDate;

	@Column(name = "next_billing_date")
	private LocalDateTime nextBillingDate;

	@Column(nullable = false)
	private LocalDateTime createdDate;

	@Column(nullable = false)
	private LocalDateTime modifiedDate;

	protected Business() {
	}

	public Business(String name, TrackerUser owner) {
		this.name = name;
		this.owner = owner;
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();

		if (createdDate == null) {
			createdDate = now;
		}

		if (businessPlan == null) {
			businessPlan = BusinessPlan.FREE_TRIAL;
		}

		if (businessStatus == null) {
			businessStatus = BusinessStatus.TRIAL;
		}

		if (trialStartDate == null) {
			trialStartDate = now;
		}

		if (trialEndDate == null) {
			trialEndDate = trialStartDate.plusDays(FREE_TRIAL_DAYS);
		}

		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		if (businessPlan == null) {
			businessPlan = BusinessPlan.FREE_TRIAL;
		}

		if (businessStatus == null) {
			businessStatus = BusinessStatus.TRIAL;
		}

		modifiedDate = LocalDateTime.now();
	}

	public void activatePaidPlan(
			BusinessPlan plan,
			LocalDateTime periodStart,
			LocalDateTime periodEnd,
			String paypalSubscriptionId,
			String paypalSubscriptionToken,
			String paypalPlanId) {
		this.businessPlan = plan;
		this.businessStatus = BusinessStatus.ACTIVE;
		this.subscriptionStartDate = periodStart;
		this.subscriptionEndDate = periodEnd;
		this.currentBillingPeriodStartDate = periodStart;
		this.currentBillingPeriodEndDate = periodEnd;
		this.lastPaymentDate = periodStart;
		this.nextBillingDate = periodEnd;
		this.paypalSubscriptionId = paypalSubscriptionId;
		this.paypalSubscriptionToken = paypalSubscriptionToken;
		this.paypalPlanId = paypalPlanId;
	}

	public void extendBillingPeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
		this.businessStatus = BusinessStatus.ACTIVE;
		this.currentBillingPeriodStartDate = periodStart;
		this.currentBillingPeriodEndDate = periodEnd;
		this.lastPaymentDate = periodStart;
		this.nextBillingDate = periodEnd;
	}

	public void markPastDue() {
		this.businessStatus = BusinessStatus.PAST_DUE;
	}

	public void deactivate() {
		this.businessStatus = BusinessStatus.DEACTIVATED;
	}

	public void cancel() {
		this.businessStatus = BusinessStatus.CANCELLED;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TrackerUser getOwner() {
		return owner;
	}

	public void setOwner(TrackerUser owner) {
		this.owner = owner;
	}

	public BusinessPlan getBusinessPlan() {
		return businessPlan;
	}

	public void setBusinessPlan(BusinessPlan businessPlan) {
		this.businessPlan = businessPlan;
	}

	public BusinessStatus getBusinessStatus() {
		return businessStatus;
	}

	public void setBusinessStatus(BusinessStatus businessStatus) {
		this.businessStatus = businessStatus;
	}

	public LocalDateTime getTrialStartDate() {
		return trialStartDate;
	}

	public void setTrialStartDate(LocalDateTime trialStartDate) {
		this.trialStartDate = trialStartDate;
	}

	public LocalDateTime getTrialEndDate() {
		return trialEndDate;
	}

	public void setTrialEndDate(LocalDateTime trialEndDate) {
		this.trialEndDate = trialEndDate;
	}

	public LocalDateTime getSubscriptionStartDate() {
		return subscriptionStartDate;
	}

	public void setSubscriptionStartDate(LocalDateTime subscriptionStartDate) {
		this.subscriptionStartDate = subscriptionStartDate;
	}

	public LocalDateTime getSubscriptionEndDate() {
		return subscriptionEndDate;
	}

	public void setSubscriptionEndDate(LocalDateTime subscriptionEndDate) {
		this.subscriptionEndDate = subscriptionEndDate;
	}

	public LocalDateTime getCurrentBillingPeriodStartDate() {
		return currentBillingPeriodStartDate;
	}

	public void setCurrentBillingPeriodStartDate(LocalDateTime currentBillingPeriodStartDate) {
		this.currentBillingPeriodStartDate = currentBillingPeriodStartDate;
	}

	public LocalDateTime getCurrentBillingPeriodEndDate() {
		return currentBillingPeriodEndDate;
	}

	public void setCurrentBillingPeriodEndDate(LocalDateTime currentBillingPeriodEndDate) {
		this.currentBillingPeriodEndDate = currentBillingPeriodEndDate;
	}

	public String getPaypalSubscriptionId() {
		return paypalSubscriptionId;
	}

	public void setPaypalSubscriptionId(String paypalSubscriptionId) {
		this.paypalSubscriptionId = paypalSubscriptionId;
	}

	public String getPaypalSubscriptionToken() {
		return paypalSubscriptionToken;
	}

	public void setPaypalSubscriptionToken(String paypalSubscriptionToken) {
		this.paypalSubscriptionToken = paypalSubscriptionToken;
	}

	public String getPaypalPlanId() {
		return paypalPlanId;
	}

	public void setPaypalPlanId(String paypalPlanId) {
		this.paypalPlanId = paypalPlanId;
	}

	public LocalDateTime getLastPaymentDate() {
		return lastPaymentDate;
	}

	public void setLastPaymentDate(LocalDateTime lastPaymentDate) {
		this.lastPaymentDate = lastPaymentDate;
	}

	public LocalDateTime getNextBillingDate() {
		return nextBillingDate;
	}

	public void setNextBillingDate(LocalDateTime nextBillingDate) {
		this.nextBillingDate = nextBillingDate;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}
}
