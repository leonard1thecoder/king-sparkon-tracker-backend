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

@Entity
@Table(name = "affiliate_posters")
public class AffiliatePoster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AffiliatePosterCategory category;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(length = 1200)
	private String description;

	@Column(name = "image_url", nullable = false, length = 2048)
	private String imageUrl;

	@Column(name = "storage_object_name", nullable = false, length = 1024)
	private String storageObjectName;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_by", nullable = false, length = 160)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected AffiliatePoster() {
	}

	public AffiliatePoster(
			AffiliatePosterCategory category,
			String title,
			String description,
			String imageUrl,
			String storageObjectName,
			String createdBy) {
		this.category = category;
		this.title = title;
		this.description = description;
		this.imageUrl = imageUrl;
		this.storageObjectName = storageObjectName;
		this.createdBy = createdBy;
		this.active = true;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	public void deactivate() {
		this.active = false;
	}

	public Long getId() {
		return id;
	}

	public AffiliatePosterCategory getCategory() {
		return category;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getStorageObjectName() {
		return storageObjectName;
	}

	public boolean isActive() {
		return active;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
