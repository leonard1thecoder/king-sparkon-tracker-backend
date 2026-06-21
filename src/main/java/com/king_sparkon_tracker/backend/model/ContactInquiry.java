package com.king_sparkon_tracker.backend.model;

import java.time.LocalDateTime;

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
@Table(name = "contact_inquiries")
public class ContactInquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 160)
	private String contactName;

	@Column(nullable = false, length = 255)
	private String businessName;

	@Column(nullable = false, length = 255)
	private String emailAddress;

	@Column(length = 64)
	private String phoneNumber;

	@Column(nullable = false, length = 2000)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ContactInquiryStatus status = ContactInquiryStatus.RECEIVED;

	@Column(nullable = false)
	private boolean confirmationEmailSent;

	@Column(nullable = false)
	private boolean notificationEmailSent;

	@Column(length = 1000)
	private String failureReason;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected ContactInquiry() {
	}

	public ContactInquiry(
			String contactName,
			String businessName,
			String emailAddress,
			String phoneNumber,
			String message) {
		this.contactName = contactName;
		this.businessName = businessName;
		this.emailAddress = emailAddress;
		this.phoneNumber = phoneNumber;
		this.message = message;
	}

	@PrePersist
	void beforeCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void markEmailSent(boolean confirmationEmailSent, boolean notificationEmailSent) {
		this.confirmationEmailSent = confirmationEmailSent;
		this.notificationEmailSent = notificationEmailSent;
		this.failureReason = null;
		this.status = ContactInquiryStatus.EMAIL_SENT;
	}

	public void markEmailFailed(
			boolean confirmationEmailSent,
			boolean notificationEmailSent,
			String failureReason) {
		this.confirmationEmailSent = confirmationEmailSent;
		this.notificationEmailSent = notificationEmailSent;
		this.failureReason = truncate(failureReason, 1000);
		this.status = ContactInquiryStatus.EMAIL_FAILED;
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	public Long getId() {
		return id;
	}

	public String getContactName() {
		return contactName;
	}

	public String getBusinessName() {
		return businessName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getMessage() {
		return message;
	}

	public ContactInquiryStatus getStatus() {
		return status;
	}

	public boolean isConfirmationEmailSent() {
		return confirmationEmailSent;
	}

	public boolean isNotificationEmailSent() {
		return notificationEmailSent;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
