package com.king_sparkon_tracker.backend.idempotency;

import java.time.Instant;

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
import jakarta.persistence.Version;

@Entity
@Table(
		name = "api_idempotency_records",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_api_idempotency_scope_actor_key",
				columnNames = { "request_scope", "actor_username", "idempotency_key" }))
public class IdempotencyRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "idempotency_key", nullable = false, length = 128)
	private String idempotencyKey;

	@Column(name = "request_scope", nullable = false, length = 96)
	private String requestScope;

	@Column(name = "actor_username", nullable = false, length = 255)
	private String actorUsername;

	@Column(name = "business_id")
	private Long businessId;

	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private IdempotencyStatus status;

	@Column(name = "response_body", columnDefinition = "text")
	private String responseBody;

	@Column(name = "response_type", length = 512)
	private String responseType;

	@Column(name = "http_status")
	private Integer httpStatus;

	@Column(name = "failure_reason", length = 1000)
	private String failureReason;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected IdempotencyRecord() {
	}

	public IdempotencyRecord(
			String idempotencyKey,
			String requestScope,
			String actorUsername,
			Long businessId,
			String requestHash,
			Instant expiresAt) {
		this.idempotencyKey = idempotencyKey;
		this.requestScope = requestScope;
		this.actorUsername = actorUsername;
		this.businessId = businessId;
		this.requestHash = requestHash;
		this.expiresAt = expiresAt;
		this.status = IdempotencyStatus.PROCESSING;
	}

	@PrePersist
	void beforeCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = Instant.now();
	}

	public void restart(String requestHash, Instant expiresAt) {
		this.requestHash = requestHash;
		this.expiresAt = expiresAt;
		this.status = IdempotencyStatus.PROCESSING;
		this.responseBody = null;
		this.responseType = null;
		this.httpStatus = null;
		this.failureReason = null;
	}

	public void complete(String responseBody, String responseType, int httpStatus) {
		this.responseBody = responseBody;
		this.responseType = responseType;
		this.httpStatus = httpStatus;
		this.failureReason = null;
		this.status = IdempotencyStatus.COMPLETED;
	}

	public void fail(String failureReason) {
		this.failureReason = failureReason == null ? "Request failed" : failureReason.substring(0, Math.min(1000, failureReason.length()));
		this.status = IdempotencyStatus.FAILED;
	}

	public Long getId() { return id; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public String getRequestScope() { return requestScope; }
	public String getActorUsername() { return actorUsername; }
	public Long getBusinessId() { return businessId; }
	public String getRequestHash() { return requestHash; }
	public IdempotencyStatus getStatus() { return status; }
	public String getResponseBody() { return responseBody; }
	public String getResponseType() { return responseType; }
	public Integer getHttpStatus() { return httpStatus; }
	public String getFailureReason() { return failureReason; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
