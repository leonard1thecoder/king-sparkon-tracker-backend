package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "stripe_webhook_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_stripe_webhook_events_event_id",
				columnNames = "stripe_event_id"
		)
)
public class StripeWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "stripe_event_id", nullable = false, unique = true)
	private String stripeEventId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(name = "stripe_subscription_id")
	private String stripeSubscriptionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private StripeWebhookProcessingStatus status = StripeWebhookProcessingStatus.RECEIVED;

	@Column(name = "failure_reason", length = 2048)
	private String failureReason;

	@Column(name = "raw_payload", columnDefinition = "text")
	private String rawPayload;

	@Column(name = "created_date", nullable = false)
	private LocalDateTime createdDate;

	@Column(name = "processed_date")
	private LocalDateTime processedDate;

	protected StripeWebhookEvent() {
	}

	public StripeWebhookEvent(
			String stripeEventId,
			String eventType,
			String stripeSubscriptionId,
			String rawPayload) {
		this.stripeEventId = stripeEventId;
		this.eventType = eventType;
		this.stripeSubscriptionId = stripeSubscriptionId;
		this.rawPayload = rawPayload;
		this.status = StripeWebhookProcessingStatus.RECEIVED;
	}

	@PrePersist
	void beforeCreate() {
		if (createdDate == null) {
			createdDate = LocalDateTime.now();
		}
	}

	public void receivedForRetry() {
		this.status = StripeWebhookProcessingStatus.RECEIVED;
		this.failureReason = null;
		this.processedDate = null;
	}

	public void processed() {
		this.status = StripeWebhookProcessingStatus.PROCESSED;
		this.failureReason = null;
		this.processedDate = LocalDateTime.now();
	}

	public void duplicate() {
		this.status = StripeWebhookProcessingStatus.DUPLICATE;
		this.processedDate = LocalDateTime.now();
	}

	public void ignored() {
		this.status = StripeWebhookProcessingStatus.IGNORED;
		this.failureReason = null;
		this.processedDate = LocalDateTime.now();
	}

	public void failed(String failureReason) {
		this.status = StripeWebhookProcessingStatus.FAILED;
		this.failureReason = failureReason;
		this.processedDate = LocalDateTime.now();
	}

	public void signatureFailed(String failureReason) {
		this.status = StripeWebhookProcessingStatus.SIGNATURE_FAILED;
		this.failureReason = failureReason;
		this.processedDate = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getStripeEventId() {
		return stripeEventId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getStripeSubscriptionId() {
		return stripeSubscriptionId;
	}

	public StripeWebhookProcessingStatus getStatus() {
		return status;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public LocalDateTime getProcessedDate() {
		return processedDate;
	}
}
