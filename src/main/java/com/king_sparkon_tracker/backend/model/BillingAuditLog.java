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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "billing_audit_logs")
public class BillingAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "business_id")
	private Business business;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64)
	private BillingAuditAction action;

	@Column(name = "actor_username")
	private String actorUsername;

	@Column(name = "paypal_event_id")
	private String paypalEventId;

	@Column(name = "paypal_subscription_id")
	private String paypalSubscriptionId;

	@Column(length = 2048)
	private String message;

	@Column(name = "created_date", nullable = false)
	private LocalDateTime createdDate;

	protected BillingAuditLog() {
	}

	public BillingAuditLog(
			Business business,
			BillingAuditAction action,
			String actorUsername,
			String paypalEventId,
			String paypalSubscriptionId,
			String message) {
		this.business = business;
		this.action = action;
		this.actorUsername = actorUsername;
		this.paypalEventId = paypalEventId;
		this.paypalSubscriptionId = paypalSubscriptionId;
		this.message = message;
	}

	@PrePersist
	void beforeCreate() {
		if (createdDate == null) {
			createdDate = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public BillingAuditAction getAction() {
		return action;
	}

	public String getActorUsername() {
		return actorUsername;
	}

	public String getPaypalEventId() {
		return paypalEventId;
	}

	public String getPaypalSubscriptionId() {
		return paypalSubscriptionId;
	}

	public String getMessage() {
		return message;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}
}
