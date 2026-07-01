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
@Table(name = "job_interviews", uniqueConstraints = {
		@UniqueConstraint(name = "uk_job_interviews_application", columnNames = "application_id")
})
public class JobInterview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "application_id", nullable = false, unique = true)
	private JobApplication application;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "job_post_id", nullable = false)
	private JobPost jobPost;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "applicant_id", nullable = false)
	private TrackerUser applicant;

	@Column(name = "interview_date", nullable = false)
	private OffsetDateTime interviewDate;

	@Column(name = "interview_description", nullable = false, length = 2000)
	private String interviewDescription;

	@Column(name = "interview_expires_at", nullable = false)
	private OffsetDateTime interviewExpiresAt;

	@Column(name = "business_address", nullable = false, length = 1024)
	private String businessAddress;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private JobInterviewStatus status = JobInterviewStatus.BOOKED;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	@Column(name = "responded_date")
	private OffsetDateTime respondedDate;

	protected JobInterview() {
	}

	public JobInterview(
			JobApplication application,
			OffsetDateTime interviewDate,
			String interviewDescription,
			OffsetDateTime interviewExpiresAt,
			String businessAddress) {
		this.application = application;
		this.jobPost = application.getJobPost();
		this.business = application.getJobPost().getBusiness();
		this.applicant = application.getApplicant();
		this.interviewDate = interviewDate;
		this.interviewDescription = interviewDescription;
		this.interviewExpiresAt = interviewExpiresAt;
		this.businessAddress = businessAddress;
		this.status = JobInterviewStatus.BOOKED;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		if (status == null) {
			status = JobInterviewStatus.BOOKED;
		}
		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		if (status == null) {
			status = JobInterviewStatus.BOOKED;
		}
		modifiedDate = OffsetDateTime.now();
	}

	public void accept() {
		status = JobInterviewStatus.ACCEPTED;
		respondedDate = OffsetDateTime.now();
	}

	public void decline() {
		status = JobInterviewStatus.DECLINED;
		respondedDate = OffsetDateTime.now();
	}

	public void expire() {
		status = JobInterviewStatus.EXPIRED;
		respondedDate = OffsetDateTime.now();
	}

	public boolean isExpired(OffsetDateTime now) {
		return interviewExpiresAt.isBefore(now);
	}

	public Long getId() {
		return id;
	}

	public JobApplication getApplication() {
		return application;
	}

	public JobPost getJobPost() {
		return jobPost;
	}

	public Business getBusiness() {
		return business;
	}

	public TrackerUser getApplicant() {
		return applicant;
	}

	public OffsetDateTime getInterviewDate() {
		return interviewDate;
	}

	public String getInterviewDescription() {
		return interviewDescription;
	}

	public OffsetDateTime getInterviewExpiresAt() {
		return interviewExpiresAt;
	}

	public String getBusinessAddress() {
		return businessAddress;
	}

	public JobInterviewStatus getStatus() {
		return status;
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
