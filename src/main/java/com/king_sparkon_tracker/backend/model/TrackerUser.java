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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tracker_users", uniqueConstraints = {
		@UniqueConstraint(name = "uk_tracker_users_username", columnNames = "username"),
		@UniqueConstraint(name = "uk_tracker_users_email_address", columnNames = "email_address")
})
public class TrackerUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(name = "email_address", nullable = false, unique = true)
	private String emailAddress;

	@Column(nullable = false)
	private LocalDateTime createdDate;

	@Column(nullable = false)
	private LocalDateTime modifiedDate;

	@Column(nullable = false)
	private String password;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "privilege_id", nullable = false)
	private Privilege privilege;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "business_id")
	private Business business;

	@Enumerated(EnumType.STRING)
	@Column(name = "localization_country", nullable = false, length = 32)
	private LocalizationCountry localizationCountry = LocalizationCountry.SOUTH_AFRICA;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "email_verified_at")
	private LocalDateTime emailVerifiedAt;

	@Column(name = "physical_address", length = 1024)
	private String physicalAddress;

	@Column(name = "cellphone_number", length = 32)
	private String cellphoneNumber;

	@Column(name = "job_title", length = 120)
	private String jobTitle;

	@Column(name = "onboarding_completed", nullable = false)
	private boolean onboardingCompleted;

	@Column(name = "tip_qr_code_enabled", nullable = false)
	private boolean tipQrCodeEnabled;

	@Column(name = "tip_qr_code_url", length = 2048)
	private String tipQrCodeUrl;

	@Column(name = "affiliate_code", unique = true, length = 64)
	private String affiliateCode;

	@Column(name = "affiliate_promotion_url", length = 2048)
	private String affiliatePromotionUrl;

	@Column(name = "affiliate_qr_code_url", length = 2048)
	private String affiliateQrCodeUrl;

	@Column(name = "affiliate_paypal_link", length = 2048)
	private String affiliatePaypalLink;

	@Column(name = "affiliate_joined_at")
	private LocalDateTime affiliateJoinedAt;

	protected TrackerUser() {
	}

	public TrackerUser(String username, String emailAddress, String password, Privilege privilege) {
		this(username, emailAddress, password, privilege, LocalizationCountry.SOUTH_AFRICA);
	}

	public TrackerUser(
			String username,
			String emailAddress,
			String password,
			Privilege privilege,
			LocalizationCountry localizationCountry) {
		this.username = username;
		this.emailAddress = emailAddress;
		this.password = password;
		this.privilege = privilege;
		this.localizationCountry = localizationCountry == null
				? LocalizationCountry.SOUTH_AFRICA
				: localizationCountry;
		this.emailVerified = false;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public LocalDateTime getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void markEmailVerified() {
		this.emailVerified = true;
		this.emailVerifiedAt = LocalDateTime.now();
	}

	public void completeOnboarding(String physicalAddress, String cellphoneNumber) {
		this.physicalAddress = physicalAddress;
		this.cellphoneNumber = cellphoneNumber;
		this.onboardingCompleted = true;
	}

	public void updateWorkerProfile(String jobTitle, boolean tipQrCodeEnabled) {
		this.jobTitle = jobTitle;
		this.tipQrCodeEnabled = tipQrCodeEnabled;
		if (!tipQrCodeEnabled) {
			this.tipQrCodeUrl = null;
		}
	}

	public void assignTipQrCodeUrl(String tipQrCodeUrl) {
		this.tipQrCodeUrl = tipQrCodeUrl;
	}

	public void activateAffiliateProfile(String affiliateCode, String promotionUrl, String qrCodeUrl) {
		this.affiliateCode = affiliateCode;
		this.affiliatePromotionUrl = promotionUrl;
		this.affiliateQrCodeUrl = qrCodeUrl;
		if (this.affiliateJoinedAt == null) {
			this.affiliateJoinedAt = LocalDateTime.now();
		}
	}

	public void completeAffiliateOnboarding(String physicalAddress, String cellphoneNumber, String paypalLink) {
		completeOnboarding(physicalAddress, cellphoneNumber);
		this.affiliatePaypalLink = paypalLink;
		this.onboardingCompleted = physicalAddress != null
				&& !physicalAddress.isBlank()
				&& cellphoneNumber != null
				&& !cellphoneNumber.isBlank()
				&& paypalLink != null
				&& !paypalLink.isBlank();
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();

		if (createdDate == null) {
			createdDate = now;
		}

		if (localizationCountry == null) {
			localizationCountry = LocalizationCountry.SOUTH_AFRICA;
		}
		syncOnboardingCompleted();

		modifiedDate = now;
	}

	@PreUpdate
	void beforeUpdate() {
		if (localizationCountry == null) {
			localizationCountry = LocalizationCountry.SOUTH_AFRICA;
		}
		syncOnboardingCompleted();

		modifiedDate = LocalDateTime.now();
	}

	private void syncOnboardingCompleted() {
		if (physicalAddress == null || cellphoneNumber == null || physicalAddress.isBlank() || cellphoneNumber.isBlank()) {
			onboardingCompleted = false;
			return;
		}
		if (privilege != null
				&& privilege.getName() == PrivilegeRole.Affiliate
				&& (affiliatePaypalLink == null || affiliatePaypalLink.isBlank())) {
			onboardingCompleted = false;
			return;
		}
		onboardingCompleted = true;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Privilege getPrivilege() {
		return privilege;
	}

	public void setPrivilege(Privilege privilege) {
		this.privilege = privilege;
	}

	public Business getBusiness() {
		return business;
	}

	public void setBusiness(Business business) {
		this.business = business;
	}

	public LocalizationCountry getLocalizationCountry() {
		return localizationCountry;
	}

	public void setLocalizationCountry(LocalizationCountry localizationCountry) {
		this.localizationCountry = localizationCountry == null
				? LocalizationCountry.SOUTH_AFRICA
				: localizationCountry;
	}

	public String getPhysicalAddress() {
		return physicalAddress;
	}

	public String getCellphoneNumber() {
		return cellphoneNumber;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public boolean isOnboardingCompleted() {
		return onboardingCompleted;
	}

	public boolean isOnboardingRequired() {
		return !onboardingCompleted;
	}

	public boolean isTipQrCodeEnabled() {
		return tipQrCodeEnabled;
	}

	public String getTipQrCodeUrl() {
		return tipQrCodeUrl;
	}

	public String getAffiliateCode() {
		return affiliateCode;
	}

	public String getAffiliatePromotionUrl() {
		return affiliatePromotionUrl;
	}

	public String getAffiliateQrCodeUrl() {
		return affiliateQrCodeUrl;
	}

	public String getAffiliatePaypalLink() {
		return affiliatePaypalLink;
	}

	public LocalDateTime getAffiliateJoinedAt() {
		return affiliateJoinedAt;
	}
}
