package com.king_sparkon_tracker.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_promotions")
public class ProductPromotion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "business_id", nullable = false)
	private Business business;

	@Column(name = "promotion_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal promotionPrice;

	@Column(name = "business_account_entry_id")
	private Long businessAccountEntryId;

	@Column(name = "starts_at", nullable = false)
	private OffsetDateTime startsAt;

	@Column(name = "ends_at", nullable = false)
	private OffsetDateTime endsAt;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_by", nullable = false, length = 255)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected ProductPromotion() {
	}

	public ProductPromotion(
			Product product,
			Business business,
			BigDecimal promotionPrice,
			Long businessAccountEntryId,
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			String createdBy) {
		this.product = product;
		this.business = business;
		this.promotionPrice = promotionPrice;
		this.businessAccountEntryId = businessAccountEntryId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.createdBy = createdBy;
		this.active = true;
	}

	@PrePersist
	void beforeCreate() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}

	public void deactivate() {
		this.active = false;
	}

	public boolean isCurrentlyActive(OffsetDateTime now) {
		return active && !startsAt.isAfter(now) && endsAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public Business getBusiness() {
		return business;
	}

	public BigDecimal getPromotionPrice() {
		return promotionPrice;
	}

	public Long getBusinessAccountEntryId() {
		return businessAccountEntryId;
	}

	public OffsetDateTime getStartsAt() {
		return startsAt;
	}

	public OffsetDateTime getEndsAt() {
		return endsAt;
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
}
