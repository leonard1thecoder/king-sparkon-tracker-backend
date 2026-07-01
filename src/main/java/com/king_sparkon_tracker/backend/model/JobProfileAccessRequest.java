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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "job_profile_access_requests", uniqueConstraints = {
		@UniqueConstraint(name = "uk_job_profile_access_application", columnNames = "application_id")
})
public class JobProfileAccessRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "application_id", nullable = false, unique = true)
	private JobApplication application;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private TrackerUser owner;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "applicant_id", nullable = false)
	private TrackerUser applicant;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private JobProfileAccessRequestStatus status = JobProfileAccessRequestStatus.REQUESTED;

	@Column(name = "request_message", length = 1000)
	private String requestMessage;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	@Column(name = "responded_date")
	private OffsetDateTime respondedDate;

	protected JobProfileAccessRequest() {
	}

	public JobProfileAccessRequest(JobApplication application, TrackerUser owner, String requestMessage) {
		this.application = application;
		this.business = application.getJobPost().getBusiness();
		this.owner = owner;
		this.applicant = application.getApplicant();
		this.requestMessage = requestMessage;
		this.status = JobProfileAccessRequestStatus.REQUESTED;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		if (status == null) {
			status = JobProfileAccessRequestStatus.REQUESTED;
		}
		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		if (status == null) {
			status = JobProfileAccessRequestStatus.REQUESTED;
		}
		modifiedDate = OffsetDateTime.now();
	}

	public void approve() {
		status = JobProfileAccessRequestStatus.APPROVED;
		respondedDate = OffsetDateTime.now();
	}

	public void decline() {
		status = JobProfileAccessRequestStatus.DECLINED;
		respondedDate = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public JobApplication getApplication() {
		return application;
	}

	public Business getBusiness() {
		return business;
	}

	public TrackerUser getOwner() {
		return owner;
	}

	public TrackerUser getApplicant() {
		return applicant;
	}

	public JobProfileAccessRequestStatus getStatus() {
		return status;
	}

	public String getRequestMessage() {
		return requestMessage;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}

	public OffsetDateTime getRespondedDate() {
		return respondedDate;
	}
}
