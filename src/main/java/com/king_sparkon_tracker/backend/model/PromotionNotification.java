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
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion_notifications")
public class PromotionNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "promotion_id", nullable = false)
	private Promotion promotion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subscriber_id", nullable = false)
	private Subscriber subscriber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PromotionChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PromotionNotificationStatus status;

	@Column(name = "failure_reason", length = 1000)
	private String failureReason;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	protected PromotionNotification() {
	}

	public PromotionNotification(
			Promotion promotion,
			Subscriber subscriber,
			PromotionChannel channel,
			PromotionNotificationStatus status,
			String failureReason) {
		this.promotion = promotion;
		this.subscriber = subscriber;
		this.channel = channel;
		this.status = status;
		this.failureReason = failureReason;
	}

	@PrePersist
	void beforeCreate() {
		if (createdDate == null) {
			createdDate = OffsetDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Promotion getPromotion() {
		return promotion;
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	public PromotionChannel getChannel() {
		return channel;
	}

	public PromotionNotificationStatus getStatus() {
		return status;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}
}
