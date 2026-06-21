package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 128)
	private String action;

	@Column(nullable = false, length = 128)
	private String entityType;

	@Column(length = 128)
	private String entityId;

	@Column(nullable = false)
	private String actorUsername;

	@Column(length = 1000)
	private String details;

	@ManyToOne
	@JoinColumn(name = "business_id")
	private Business business;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected AuditLog() {
	}

	public AuditLog(String action, String entityType, String entityId, String actorUsername, String details) {
		this(action, entityType, entityId, actorUsername, details, null);
	}

	public AuditLog(
			String action,
			String entityType,
			String entityId,
			String actorUsername,
			String details,
			Business business) {
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.actorUsername = actorUsername;
		this.details = details;
		this.business = business;
	}

	@PrePersist
	void beforeCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public String getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getActorUsername() {
		return actorUsername;
	}

	public String getDetails() {
		return details;
	}

	public Business getBusiness() {
		return business;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
