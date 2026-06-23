package com.king_sparkon_tracker.backend.model;

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
@Table(name = "owner_payout_accounts")
public class OwnerPayoutAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(name = "business_id", nullable = false, unique = true)
	private Long businessId;

	@Column(name = "paypal_email", nullable = false)
	private String paypalEmail;

	@Column(name = "onboarding_token", nullable = false)
	private String onboardingToken;

	@Column(name = "onboarding_url", nullable = false, length = 2048)
	private String onboardingUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PayoutAccountStatus status = PayoutAccountStatus.ACTIVE;

	@Column(nullable = false)
	private OffsetDateTime created;

	@Column(nullable = false)
	private OffsetDateTime updated;

	protected OwnerPayoutAccount() {
	}

	public OwnerPayoutAccount(
			Long ownerId,
			Long businessId,
			String paypalEmail,
			String onboardingToken,
			String onboardingUrl) {
		OffsetDateTime now = OffsetDateTime.now();
		this.ownerId = ownerId;
		this.businessId = businessId;
		this.paypalEmail = paypalEmail;
		this.onboardingToken = onboardingToken;
		this.onboardingUrl = onboardingUrl;
		this.status = PayoutAccountStatus.ACTIVE;
		this.created = now;
		this.updated = now;
	}

	public void update(Long ownerId, String paypalEmail, String onboardingToken, String onboardingUrl) {
		this.ownerId = ownerId;
		this.paypalEmail = paypalEmail;
		this.onboardingToken = onboardingToken;
		this.onboardingUrl = onboardingUrl;
		this.status = PayoutAccountStatus.ACTIVE;
		this.updated = OffsetDateTime.now();
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
			status = PayoutAccountStatus.ACTIVE;
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

	public String getPaypalEmail() {
		return paypalEmail;
	}

	public String getOnboardingToken() {
		return onboardingToken;
	}

	public String getOnboardingUrl() {
		return onboardingUrl;
	}

	public PayoutAccountStatus getStatus() {
		return status;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}
}
