package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;

import com.king_sparkon_tracker.backend.model.ContactInquiry;
import com.king_sparkon_tracker.backend.model.ContactInquiryStatus;

public record ContactInquiryResponse(
		Long id,
		ContactInquiryStatus status,
		boolean confirmationEmailSent,
		boolean notificationEmailSent,
		String message,
		LocalDateTime createdAt
) {

	public static ContactInquiryResponse from(ContactInquiry inquiry, String message) {
		return new ContactInquiryResponse(
				inquiry.getId(),
				inquiry.getStatus(),
				inquiry.isConfirmationEmailSent(),
				inquiry.isNotificationEmailSent(),
				message,
				inquiry.getCreatedAt());
	}
}
