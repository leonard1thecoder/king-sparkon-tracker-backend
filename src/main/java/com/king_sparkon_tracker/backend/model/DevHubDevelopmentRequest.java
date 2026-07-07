package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dev_hub_development_requests")
public class DevHubDevelopmentRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String clientName;

	@Column(nullable = false, length = 180)
	private String emailAddress;

	@Column(length = 80)
	private String phoneNumber;

	@Column(length = 180)
	private String companyName;

	@Column(nullable = false, length = 120)
	private String projectType;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(length = 120)
	private String budgetRange;

	@Column(length = 120)
	private String timeline;

	@Column(nullable = false, length = 3)
	private String currency = "ZAR";

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal estimatedMinPrice = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal estimatedMaxPrice = BigDecimal.ZERO;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String aiDevelopmentPlan;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String aiAutomatedResponse;

	@Column(columnDefinition = "TEXT")
	private String decisionReason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private DevHubRequestStatus status = DevHubRequestStatus.AI_QUOTED;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private OffsetDateTime updatedAt;

	protected DevHubDevelopmentRequest() {
	}

	public DevHubDevelopmentRequest(
			String clientName,
			String emailAddress,
			String phoneNumber,
			String companyName,
			String projectType,
			String title,
			String description,
			String budgetRange,
			String timeline) {
		this.clientName = clientName;
		this.emailAddress = emailAddress;
		this.phoneNumber = phoneNumber;
		this.companyName = companyName;
		this.projectType = projectType;
		this.title = title;
		this.description = description;
		this.budgetRange = budgetRange;
		this.timeline = timeline;
	}

	public Long getId() {
		return id;
	}

	public String getClientName() {
		return clientName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getProjectType() {
		return projectType;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getBudgetRange() {
		return budgetRange;
	}

	public String getTimeline() {
		return timeline;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getEstimatedMinPrice() {
		return estimatedMinPrice;
	}

	public BigDecimal getEstimatedMaxPrice() {
		return estimatedMaxPrice;
	}

	public String getAiDevelopmentPlan() {
		return aiDevelopmentPlan;
	}

	public String getAiAutomatedResponse() {
		return aiAutomatedResponse;
	}

	public String getDecisionReason() {
		return decisionReason;
	}

	public DevHubRequestStatus getStatus() {
		return status;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void applyAiQuote(BigDecimal minPrice, BigDecimal maxPrice, String developmentPlan, String automatedResponse) {
		this.estimatedMinPrice = minPrice;
		this.estimatedMaxPrice = maxPrice;
		this.aiDevelopmentPlan = developmentPlan;
		this.aiAutomatedResponse = automatedResponse;
		this.status = DevHubRequestStatus.AI_QUOTED;
	}

	public void accept(String reason) {
		this.status = DevHubRequestStatus.ACCEPTED;
		this.decisionReason = reason;
	}

	public void reject(String reason) {
		this.status = DevHubRequestStatus.REJECTED;
		this.decisionReason = reason;
	}
}
