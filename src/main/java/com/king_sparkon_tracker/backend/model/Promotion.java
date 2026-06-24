package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
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
@Table(name = "promotions")
public class Promotion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "business_id")
	private Business business;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(name = "landing_url", length = 2048)
	private String landingUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PromotionOrigin origin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PromotionChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PromotionStatus status = PromotionStatus.ACTIVE;

	@Column(name = "target_count", nullable = false)
	private int targetCount;

	@Column(name = "bulk_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal bulkPrice;

	@Column(name = "created_by", nullable = false, length = 255)
	private String createdBy;

	@Column(name = "scheduled_for", nullable = false)
	private OffsetDateTime scheduledFor;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	@Column(name = "last_processed_at")
	private OffsetDateTime lastProcessedAt;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	protected Promotion() {
	}

	public Promotion(
			Business business,
			String title,
			String message,
			String landingUrl,
			PromotionOrigin origin,
			PromotionChannel channel,
			int targetCount,
			BigDecimal bulkPrice,
			String createdBy,
			OffsetDateTime scheduledFor,
			OffsetDateTime expiresAt) {
		this.business = business;
		this.title = title;
		this.message = message;
		this.landingUrl = landingUrl;
		this.origin = origin;
		this.channel = channel;
		this.targetCount = targetCount;
		this.bulkPrice = bulkPrice;
		this.createdBy = createdBy;
		this.scheduledFor = scheduledFor;
		this.expiresAt = expiresAt;
		this.status = PromotionStatus.ACTIVE;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		if (modifiedDate == null) {
			modifiedDate = now;
		}
		if (scheduledFor == null) {
			scheduledFor = now;
		}
		if (status == null) {
			status = PromotionStatus.ACTIVE;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		modifiedDate = OffsetDateTime.now();
	}

	public void markProcessed(OffsetDateTime processedAt) {
		this.lastProcessedAt = processedAt;
	}

	public void expire() {
		this.status = PromotionStatus.EXPIRED;
	}

	public Long getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public String getLandingUrl() {
		return landingUrl;
	}

	public PromotionOrigin getOrigin() {
		return origin;
	}

	public PromotionChannel getChannel() {
		return channel;
	}

	public PromotionStatus getStatus() {
		return status;
	}

	public int getTargetCount() {
		return targetCount;
	}

	public BigDecimal getBulkPrice() {
		return bulkPrice;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getScheduledFor() {
		return scheduledFor;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public OffsetDateTime getLastProcessedAt() {
		return lastProcessedAt;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}
}
