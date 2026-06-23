package com.king_sparkon_tracker.backend.model;

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
@Table(name = "worker_payout_accounts")
public class WorkerPayoutAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "worker_id", nullable = false, unique = true)
	private TrackerUser worker;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private TrackerUser configuredByOwner;

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

	protected WorkerPayoutAccount() {
	}

	public WorkerPayoutAccount(
			TrackerUser worker,
			TrackerUser configuredByOwner,
			String paypalEmail,
			String onboardingToken,
			String onboardingUrl) {
		OffsetDateTime now = OffsetDateTime.now();
		this.worker = worker;
		this.configuredByOwner = configuredByOwner;
		this.paypalEmail = paypalEmail;
		this.onboardingToken = onboardingToken;
		this.onboardingUrl = onboardingUrl;
		this.status = PayoutAccountStatus.ACTIVE;
		this.created = now;
		this.updated = now;
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
			status = PayoutAccountStatus.ONBOARDING_REQUIRED;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updated = OffsetDateTime.now();
	}

	public void update(TrackerUser configuredByOwner, String paypalEmail, String onboardingToken, String onboardingUrl) {
		this.configuredByOwner = configuredByOwner;
		this.paypalEmail = paypalEmail;
		this.onboardingToken = onboardingToken;
		this.onboardingUrl = onboardingUrl;
		this.status = PayoutAccountStatus.ACTIVE;
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

	public Long getOwnerId() {
		return configuredByOwner == null ? null : configuredByOwner.getId();
	}

	public TrackerUser getConfiguredByOwner() {
		return configuredByOwner;
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
