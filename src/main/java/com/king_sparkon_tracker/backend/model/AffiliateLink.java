package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;
import java.util.Objects;

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
@Table(name = "affiliate_links")
public class AffiliateLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(length = 512)
	private String description;

	@Column(name = "affiliate_url", nullable = false, length = 2048)
	private String affiliateUrl;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Column(name = "website_name", nullable = false, length = 120)
	private String websiteName;

	@Column(length = 120)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64)
	private AffiliatePlacement placement;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AffiliateLinkStatus status = AffiliateLinkStatus.ACTIVE;

	@Column(nullable = false)
	private int priority = 1;

	@Column(name = "display_for_free_trial", nullable = false)
	private boolean displayForFreeTrial = true;

	@Column(name = "display_for_plus", nullable = false)
	private boolean displayForPlus = true;

	@Column(name = "display_for_pro", nullable = false)
	private boolean displayForPro;

	@Column(name = "impression_count", nullable = false)
	private long impressionCount;

	@Column(name = "click_count", nullable = false)
	private long clickCount;

	@Column(nullable = false)
	private LocalDateTime createdDate;

	@Column(nullable = false)
	private LocalDateTime modifiedDate;

	protected AffiliateLink() {
	}

	public AffiliateLink(
			String title,
			String description,
			String affiliateUrl,
			String imageUrl,
			String websiteName,
			String category,
			AffiliatePlacement placement,
			AffiliateLinkStatus status,
			int priority) {
		this.title = title;
		this.description = description;
		this.affiliateUrl = affiliateUrl;
		this.imageUrl = imageUrl;
		this.websiteName = websiteName;
		this.category = category;
		this.placement = placement;
		this.status = status == null ? AffiliateLinkStatus.ACTIVE : status;
		this.priority = Math.max(priority, 1);
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdDate == null) {
			createdDate = now;
		}
		modifiedDate = now;
		normalizeDefaults();
	}

	@PreUpdate
	void beforeUpdate() {
		modifiedDate = LocalDateTime.now();
		normalizeDefaults();
	}

	private void normalizeDefaults() {
		if (status == null) {
			status = AffiliateLinkStatus.ACTIVE;
		}
		priority = Math.max(priority, 1);
	}

	public boolean supportsPlan(BusinessPlan plan) {
		BusinessPlan safePlan = plan == null ? BusinessPlan.FREE_TRIAL : plan;
		return switch (safePlan) {
			case FREE_TRIAL -> displayForFreeTrial;
			case PLUS -> displayForPlus;
			case PRO -> displayForPro;
		};
	}

	public void setDisplayPlans(boolean displayForFreeTrial, boolean displayForPlus, boolean displayForPro) {
		this.displayForFreeTrial = displayForFreeTrial;
		this.displayForPlus = displayForPlus;
		this.displayForPro = displayForPro;
	}

	public void recordImpression() {
		impressionCount++;
	}

	public void recordClick() {
		clickCount++;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAffiliateUrl() {
		return affiliateUrl;
	}

	public void setAffiliateUrl(String affiliateUrl) {
		this.affiliateUrl = affiliateUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getWebsiteName() {
		return websiteName;
	}

	public void setWebsiteName(String websiteName) {
		this.websiteName = websiteName;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public AffiliatePlacement getPlacement() {
		return placement;
	}

	public void setPlacement(AffiliatePlacement placement) {
		this.placement = placement;
	}

	public AffiliateLinkStatus getStatus() {
		return status;
	}

	public void setStatus(AffiliateLinkStatus status) {
		this.status = Objects.requireNonNullElse(status, AffiliateLinkStatus.ACTIVE);
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = Math.max(priority, 1);
	}

	public boolean isDisplayForFreeTrial() {
		return displayForFreeTrial;
	}

	public boolean isDisplayForPlus() {
		return displayForPlus;
	}

	public boolean isDisplayForPro() {
		return displayForPro;
	}

	public long getImpressionCount() {
		return impressionCount;
	}

	public long getClickCount() {
		return clickCount;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}
}
