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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "developer_hub_software_requests")
public class DeveloperHubSoftwareRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private TrackerUser owner;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Column(name = "business_name", nullable = false, length = 255)
	private String businessName;

	@Column(name = "owner_name", nullable = false, length = 255)
	private String ownerName;

	@Column(name = "owner_email", nullable = false, length = 255)
	private String ownerEmail;

	@Column(name = "software_name", nullable = false, length = 160)
	private String softwareName;

	@Column(name = "software_description", nullable = false, length = 4000)
	private String softwareDescription;

	@Column(name = "requires_cloud_maintenance", nullable = false)
	private boolean requiresCloudMaintenance;

	@Column(name = "requires_quality_assurance_regression", nullable = false)
	private boolean requiresQualityAssuranceRegression;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SoftwareDevelopmentStage stage = SoftwareDevelopmentStage.REQUESTED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SoftwareDevelopmentStatus status = SoftwareDevelopmentStatus.REQUESTED;

	@Column(name = "admin_note", length = 2000)
	private String adminNote;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "quote_sent_at")
	private LocalDateTime quoteSentAt;

	protected DeveloperHubSoftwareRequest() {
	}

	public DeveloperHubSoftwareRequest(
			TrackerUser owner,
			Business business,
			String softwareName,
			String softwareDescription,
			boolean requiresCloudMaintenance,
			boolean requiresQualityAssuranceRegression) {
		this.owner = owner;
		this.business = business;
		this.businessName = business.getName();
		this.ownerName = owner.getUsername();
		this.ownerEmail = owner.getEmailAddress();
		this.softwareName = softwareName;
		this.softwareDescription = softwareDescription;
		this.requiresCloudMaintenance = requiresCloudMaintenance;
		this.requiresQualityAssuranceRegression = requiresQualityAssuranceRegression;
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (requestedAt == null) {
			requestedAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (stage == null) {
			stage = SoftwareDevelopmentStage.REQUESTED;
		}
		if (status == null) {
			status = SoftwareDevelopmentStatus.REQUESTED;
		}
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void advanceTo(SoftwareDevelopmentStage nextStage, SoftwareDevelopmentStatus nextStatus, String note) {
		this.stage = nextStage == null ? SoftwareDevelopmentStage.REQUESTED : nextStage;
		this.status = nextStatus == null ? statusForStage(this.stage) : nextStatus;
		this.adminNote = normalizeOptional(note);

		if (this.stage != SoftwareDevelopmentStage.REQUESTED && this.startedAt == null) {
			this.startedAt = LocalDateTime.now();
		}
		if (this.stage == SoftwareDevelopmentStage.QUOTE_SENT && this.quoteSentAt == null) {
			this.quoteSentAt = LocalDateTime.now();
		}
	}

	private SoftwareDevelopmentStatus statusForStage(SoftwareDevelopmentStage stage) {
		return switch (stage) {
			case REQUESTED -> SoftwareDevelopmentStatus.REQUESTED;
			case QUOTE_SENT -> SoftwareDevelopmentStatus.QUOTE_SENT;
			case APPROVED -> SoftwareDevelopmentStatus.APPROVED;
			case LIVE_SUPPORT -> SoftwareDevelopmentStatus.COMPLETED;
			default -> SoftwareDevelopmentStatus.IN_PROGRESS;
		};
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public Long getId() {
		return id;
	}

	public TrackerUser getOwner() {
		return owner;
	}

	public Business getBusiness() {
		return business;
	}

	public String getBusinessName() {
		return businessName;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public String getOwnerEmail() {
		return ownerEmail;
	}

	public String getSoftwareName() {
		return softwareName;
	}

	public String getSoftwareDescription() {
		return softwareDescription;
	}

	public boolean isRequiresCloudMaintenance() {
		return requiresCloudMaintenance;
	}

	public boolean isRequiresQualityAssuranceRegression() {
		return requiresQualityAssuranceRegression;
	}

	public SoftwareDevelopmentStage getStage() {
		return stage;
	}

	public SoftwareDevelopmentStatus getStatus() {
		return status;
	}

	public String getAdminNote() {
		return adminNote;
	}

	public LocalDateTime getRequestedAt() {
		return requestedAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getQuoteSentAt() {
		return quoteSentAt;
	}
}
