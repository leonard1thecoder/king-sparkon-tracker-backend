package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_posts")
public class JobPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private TrackerUser owner;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(name = "starting_date", nullable = false)
	private LocalDate startingDate;

	@Column(name = "closing_date", nullable = false)
	private LocalDate closingDate;

	@Column(name = "job_description", nullable = false, length = 4000)
	private String jobDescription;

	@Enumerated(EnumType.STRING)
	@Column(name = "years_of_experience_required", nullable = false, length = 40)
	private JobExperienceLevel yearsOfExperienceRequired;

	@Column(name = "job_post_file_url", length = 2048)
	private String jobPostFileUrl;

	@Column(name = "estimated_salary", precision = 14, scale = 2)
	private BigDecimal estimatedSalary;

	@Column(nullable = false, length = 8)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private JobPostStatus status = JobPostStatus.OPEN;

	@Column(nullable = false)
	private OffsetDateTime createdDate;

	@Column(nullable = false)
	private OffsetDateTime modifiedDate;

	protected JobPost() {
	}

	public JobPost(
			Business business,
			TrackerUser owner,
			String title,
			LocalDate startingDate,
			LocalDate closingDate,
			String jobDescription,
			JobExperienceLevel yearsOfExperienceRequired,
			String jobPostFileUrl,
			BigDecimal estimatedSalary,
			String currency) {
		this.business = business;
		this.owner = owner;
		this.title = title;
		this.startingDate = startingDate;
		this.closingDate = closingDate;
		this.jobDescription = jobDescription;
		this.yearsOfExperienceRequired = yearsOfExperienceRequired;
		this.jobPostFileUrl = jobPostFileUrl;
		this.estimatedSalary = estimatedSalary;
		this.currency = currency;
		this.status = JobPostStatus.OPEN;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		if (status == null) {
			status = JobPostStatus.OPEN;
		}
		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		if (status == null) {
			status = JobPostStatus.OPEN;
		}
		modifiedDate = OffsetDateTime.now();
	}

	public boolean isOpen(LocalDate today) {
		return status == JobPostStatus.OPEN && !closingDate.isBefore(today);
	}

	public void close() {
		this.status = JobPostStatus.CLOSED;
	}

	public Long getId() {
		return id;
	}

	public Business getBusiness() {
		return business;
	}

	public TrackerUser getOwner() {
		return owner;
	}

	public String getTitle() {
		return title;
	}

	public LocalDate getStartingDate() {
		return startingDate;
	}

	public LocalDate getClosingDate() {
		return closingDate;
	}

	public String getJobDescription() {
		return jobDescription;
	}

	public JobExperienceLevel getYearsOfExperienceRequired() {
		return yearsOfExperienceRequired;
	}

	public String getJobPostFileUrl() {
		return jobPostFileUrl;
	}

	public BigDecimal getEstimatedSalary() {
		return estimatedSalary;
	}

	public String getCurrency() {
		return currency;
	}

	public JobPostStatus getStatus() {
		return status;
	}

	public OffsetDateTime getCreatedDate() {
		return createdDate;
	}

	public OffsetDateTime getModifiedDate() {
		return modifiedDate;
	}
}
