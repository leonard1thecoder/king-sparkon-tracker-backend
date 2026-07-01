package com.king_sparkon_tracker.backend.model;

import java.time.OffsetDateTime;
import java.util.List;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "job_applications", uniqueConstraints = {
		@UniqueConstraint(name = "uk_job_applications_post_applicant", columnNames = {"job_post_id", "applicant_id"})
})
public class JobApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "job_post_id", nullable = false)
	private JobPost jobPost;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "applicant_id", nullable = false)
	private TrackerUser applicant;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private JobSeekerProfile profile;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private JobApplicationStatus status = JobApplicationStatus.SUBMITTED;

	@Column(name = "resume_url", nullable = false, length = 2048)
	private String resumeUrl;

	@Column(name = "certificate_urls", length = 4000)
	private String certificateUrls;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	@Column(name = "viewed_date")
	private OffsetDateTime viewedDate;

	@Column(name = "decision_date")
	private OffsetDateTime decisionDate;

	protected JobApplication() {
	}

	public JobApplication(JobPost jobPost, TrackerUser applicant, JobSeekerProfile profile, String resumeUrl, List<String> certificateUrls) {
		this.jobPost = jobPost;
		this.applicant = applicant;
		this.profile = profile;
		this.resumeUrl = resumeUrl;
		this.certificateUrls = certificateUrls == null || certificateUrls.isEmpty() ? null : String.join(",", certificateUrls);
		this.status = JobApplicationStatus.SUBMITTED;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		if (status == null) {
			status = JobApplicationStatus.SUBMITTED;
		}
		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		if (status == null) {
			status = JobApplicationStatus.SUBMITTED;
		}
		modifiedDate = OffsetDateTime.now();
	}

	public void markViewed() {
		if (status == JobApplicationStatus.SUBMITTED) {
			status = JobApplicationStatus.VIEWED;
		}
		viewedDate = OffsetDateTime.now();
	}

	public void reject() {
		status = JobApplicationStatus.REJECTED;
		decisionDate = OffsetDateTime.now();
	}

	public void markAccepted() {
		status = JobApplicationStatus.ACCEPTED;
		decisionDate = OffsetDateTime.now();
	}

	public void markInterviewBooked() {
		status = JobApplicationStatus.INTERVIEW_BOOKED;
		decisionDate = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public JobPost getJobPost() {
		return jobPost;
	}

	public TrackerUser getApplicant() {
		return applicant;
	}

	public JobSeekerProfile getProfile() {
		return profile;
	}

	public JobApplicationStatus getStatus() {
		return status;
	}

	public String getResumeUrl() {
		return resumeUrl;
	}

	public List<String> getCertificateUrls() {
		if (certificateUrls == null || certificateUrls.isBlank()) {
			return List.of();
		}
		return List.of(certificateUrls.split(","));
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}

	public OffsetDateTime getViewedDate() {
		return viewedDate;
	}

	public OffsetDateTime getDecisionDate() {
		return decisionDate;
	}
}
