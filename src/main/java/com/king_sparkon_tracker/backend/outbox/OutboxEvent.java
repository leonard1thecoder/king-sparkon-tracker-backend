package com.king_sparkon_tracker.backend.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "outbox_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_outbox_deduplication_key",
				columnNames = "deduplication_key"))
public class OutboxEvent {

	@Id
	@Column(length = 64)
	private String id;

	@Column(name = "aggregate_type", nullable = false, length = 80)
	private String aggregateType;

	@Column(name = "aggregate_id", length = 128)
	private String aggregateId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 96)
	private OutboxEventType eventType;

	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private OutboxStatus status;

	@Column(name = "deduplication_key", nullable = false, length = 180)
	private String deduplicationKey;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "max_attempts", nullable = false)
	private int maxAttempts;

	@Column(name = "available_at", nullable = false)
	private Instant availableAt;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(name = "last_error", length = 2000)
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected OutboxEvent() {
	}

	public OutboxEvent(
			String aggregateType,
			String aggregateId,
			OutboxEventType eventType,
			String payload,
			String deduplicationKey,
			Instant availableAt) {
		this.id = "OUT-" + UUID.randomUUID();
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.deduplicationKey = deduplicationKey;
		this.availableAt = availableAt == null ? Instant.now() : availableAt;
		this.status = OutboxStatus.PENDING;
		this.maxAttempts = 10;
	}

	@PrePersist
	void beforeCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() { updatedAt = Instant.now(); }

	public void claim() {
		status = OutboxStatus.PROCESSING;
		lockedAt = Instant.now();
		attempts++;
	}

	public void processed() {
		status = OutboxStatus.PROCESSED;
		processedAt = Instant.now();
		lastError = null;
		lockedAt = null;
	}

	public void retry(Throwable error) {
		lastError = safeMessage(error);
		lockedAt = null;
		if (attempts >= maxAttempts) {
			status = OutboxStatus.DEAD_LETTER;
			return;
		}
		status = OutboxStatus.FAILED;
		long delaySeconds = Math.min(3600L, 15L * (1L << Math.min(attempts, 8)));
		availableAt = Instant.now().plusSeconds(delaySeconds);
	}

	public void releaseStaleLock() {
		if (status == OutboxStatus.PROCESSING) {
			status = OutboxStatus.FAILED;
			lockedAt = null;
			availableAt = Instant.now();
			lastError = "Stale processing lock released";
		}
	}

	private String safeMessage(Throwable error) {
		String message = error == null || error.getMessage() == null ? "Outbox processing failed" : error.getMessage();
		return message.substring(0, Math.min(message.length(), 2000));
	}

	public String getId() { return id; }
	public String getAggregateType() { return aggregateType; }
	public String getAggregateId() { return aggregateId; }
	public OutboxEventType getEventType() { return eventType; }
	public String getPayload() { return payload; }
	public OutboxStatus getStatus() { return status; }
	public String getDeduplicationKey() { return deduplicationKey; }
	public int getAttempts() { return attempts; }
	public int getMaxAttempts() { return maxAttempts; }
	public Instant getAvailableAt() { return availableAt; }
	public Instant getLockedAt() { return lockedAt; }
	public Instant getProcessedAt() { return processedAt; }
	public String getLastError() { return lastError; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
