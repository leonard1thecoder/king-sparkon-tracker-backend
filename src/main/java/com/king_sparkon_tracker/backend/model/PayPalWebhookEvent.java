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
		name = "paypal_webhook_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_paypal_webhook_events_event_id",
				columnNames = "paypal_event_id"
		)
)
public class PayPalWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "paypal_event_id", nullable = false, unique = true)
	private String paypalEventId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(name = "paypal_subscription_id")
	private String paypalSubscriptionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PayPalWebhookProcessingStatus status = PayPalWebhookProcessingStatus.RECEIVED;

	@Column(name = "failure_reason", length = 2048)
	private String failureReason;

	@Column(name = "raw_payload", columnDefinition = "text")
	private String rawPayload;

	@Column(name = "created_date", nullable = false)
	private LocalDateTime createdDate;

	@Column(name = "processed_date")
	private LocalDateTime processedDate;

	protected PayPalWebhookEvent() {
	}

	public PayPalWebhookEvent(
			String paypalEventId,
			String eventType,
			String paypalSubscriptionId,
			String rawPayload) {
		this.paypalEventId = paypalEventId;
		this.eventType = eventType;
		this.paypalSubscriptionId = paypalSubscriptionId;
		this.rawPayload = rawPayload;
		this.status = PayPalWebhookProcessingStatus.RECEIVED;
	}

	@PrePersist
	void beforeCreate() {
		if (createdDate == null) {
			createdDate = LocalDateTime.now();
		}
	}

	public void processed() {
		this.status = PayPalWebhookProcessingStatus.PROCESSED;
		this.processedDate = LocalDateTime.now();
	}

	public void duplicate() {
		this.status = PayPalWebhookProcessingStatus.DUPLICATE;
		this.processedDate = LocalDateTime.now();
	}

	public void ignored() {
		this.status = PayPalWebhookProcessingStatus.IGNORED;
		this.processedDate = LocalDateTime.now();
	}

	public void failed(String failureReason) {
		this.status = PayPalWebhookProcessingStatus.FAILED;
		this.failureReason = failureReason;
		this.processedDate = LocalDateTime.now();
	}

	public void signatureFailed(String failureReason) {
		this.status = PayPalWebhookProcessingStatus.SIGNATURE_FAILED;
		this.failureReason = failureReason;
		this.processedDate = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getPaypalEventId() {
		return paypalEventId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getPaypalSubscriptionId() {
		return paypalSubscriptionId;
	}

	public PayPalWebhookProcessingStatus getStatus() {
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
