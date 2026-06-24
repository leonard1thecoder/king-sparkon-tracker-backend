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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "subscribers", uniqueConstraints = {
		@UniqueConstraint(name = "uk_subscribers_contact_value", columnNames = "contact_value")
})
public class Subscriber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "contact_value", nullable = false, unique = true, length = 320)
	private String contactValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "contact_type", nullable = false, length = 24)
	private SubscriberContactType contactType;

	@Enumerated(EnumType.STRING)
	@Column(name = "subscriber_type", nullable = false, length = 32)
	private SubscriberType subscriberType;

	@Column(name = "affiliate_registered", nullable = false)
	private boolean affiliateRegistered;

	@Enumerated(EnumType.STRING)
	@Column(name = "preferred_channel", nullable = false, length = 16)
	private PromotionChannel preferredChannel;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "source", nullable = false, length = 64)
	private String source;

	@Column(name = "last_notified_at")
	private OffsetDateTime lastNotifiedAt;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	protected Subscriber() {
	}

	public Subscriber(
			String contactValue,
			SubscriberContactType contactType,
			SubscriberType subscriberType,
			boolean affiliateRegistered,
			PromotionChannel preferredChannel,
			String source) {
		this.contactValue = contactValue;
		this.contactType = contactType;
		this.subscriberType = subscriberType;
		this.affiliateRegistered = affiliateRegistered;
		this.preferredChannel = preferredChannel;
		this.source = source;
		this.active = true;
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
		if (subscriberType == null) {
			subscriberType = SubscriberType.KINGSPARKON_SUBSCRIBER;
		}
		if (preferredChannel == null) {
			preferredChannel = PromotionChannel.ANY;
		}
		if (source == null || source.isBlank()) {
			source = "DIRECT";
		}
	}

	@PreUpdate
	void beforeUpdate() {
		modifiedDate = OffsetDateTime.now();
	}

	public void reactivate(SubscriberType subscriberType, boolean affiliateRegistered, PromotionChannel preferredChannel, String source) {
		this.active = true;
		if (subscriberType != null) {
			this.subscriberType = subscriberType;
		}
		this.affiliateRegistered = affiliateRegistered;
		if (preferredChannel != null) {
			this.preferredChannel = preferredChannel;
		}
		if (source != null && !source.isBlank()) {
			this.source = source;
		}
	}

	public void unsubscribe() {
		this.active = false;
	}

	public void markNotified(OffsetDateTime notifiedAt) {
		this.lastNotifiedAt = notifiedAt;
	}

	public Long getId() {
		return id;
	}

	public String getContactValue() {
		return contactValue;
	}

	public SubscriberContactType getContactType() {
		return contactType;
	}

	public SubscriberType getSubscriberType() {
		return subscriberType;
	}

	public boolean isAffiliateRegistered() {
		return affiliateRegistered;
	}

	public PromotionChannel getPreferredChannel() {
		return preferredChannel;
	}

	public boolean isActive() {
		return active;
	}

	public String getSource() {
		return source;
	}

	public OffsetDateTime getLastNotifiedAt() {
		return lastNotifiedAt;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}
}
