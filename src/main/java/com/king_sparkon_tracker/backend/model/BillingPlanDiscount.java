package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "billing_plan_discounts",
		uniqueConstraints = @UniqueConstraint(name = "uk_billing_plan_discounts_plan", columnNames = "business_plan")
)
public class BillingPlanDiscount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_plan", nullable = false, length = 32)
	private BusinessPlan businessPlan;

	@Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal discountPercent;

	@Column(name = "label", nullable = false, length = 120)
	private String label;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "starts_at")
	private OffsetDateTime startsAt;

	@Column(name = "ends_at")
	private OffsetDateTime endsAt;

	@Column(name = "updated_by", nullable = false, length = 255)
	private String updatedBy;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected BillingPlanDiscount() {
	}

	public BillingPlanDiscount(BusinessPlan businessPlan) {
		this.businessPlan = businessPlan;
	}

	@PrePersist
	void beforeCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (createdAt == null) createdAt = now;
		if (updatedAt == null) updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = OffsetDateTime.now();
	}

	public void update(
			BigDecimal discountPercent,
			String label,
			boolean active,
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String updatedBy) {
		this.discountPercent = discountPercent;
		this.label = label;
		this.active = active;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.updatedBy = updatedBy;
	}

	public boolean isEffective(OffsetDateTime now) {
		if (!active) return false;
		if (startsAt != null && startsAt.isAfter(now)) return false;
		return endsAt == null || endsAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public BusinessPlan getBusinessPlan() {
		return businessPlan;
	}

	public BigDecimal getDiscountPercent() {
		return discountPercent;
	}

	public String getLabel() {
		return label;
	}

	public boolean isActive() {
		return active;
	}

	public OffsetDateTime getStartsAt() {
		return startsAt;
	}

	public OffsetDateTime getEndsAt() {
		return endsAt;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
