package com.king_sparkon_tracker.backend.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "generated_qr_assets",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_generated_qr_aggregate",
				columnNames = { "aggregate_type", "aggregate_id", "qr_value_hash" }))
public class GeneratedQrAsset {

	@Id
	@Column(length = 64)
	private String id;
	@Column(name = "aggregate_type", nullable = false, length = 80)
	private String aggregateType;
	@Column(name = "aggregate_id", nullable = false, length = 128)
	private String aggregateId;
	@Column(name = "qr_value_hash", nullable = false, length = 64)
	private String qrValueHash;
	@Column(name = "object_name", length = 512)
	private String objectName;
	@Column(name = "public_url", length = 2048)
	private String publicUrl;
	@Column(name = "content_base64", columnDefinition = "text")
	private String contentBase64;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected GeneratedQrAsset() {
	}

	public GeneratedQrAsset(
			String aggregateType,
			String aggregateId,
			String qrValueHash,
			String objectName,
			String publicUrl,
			String contentBase64) {
		this.id = "QRA-" + UUID.randomUUID();
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.qrValueHash = qrValueHash;
		this.objectName = objectName;
		this.publicUrl = publicUrl;
		this.contentBase64 = contentBase64;
	}

	@PrePersist
	void beforeCreate() { createdAt = Instant.now(); }
	public String getId() { return id; }
	public String getAggregateType() { return aggregateType; }
	public String getAggregateId() { return aggregateId; }
	public String getQrValueHash() { return qrValueHash; }
	public String getObjectName() { return objectName; }
	public String getPublicUrl() { return publicUrl; }
	public String getContentBase64() { return contentBase64; }
	public Instant getCreatedAt() { return createdAt; }
}
