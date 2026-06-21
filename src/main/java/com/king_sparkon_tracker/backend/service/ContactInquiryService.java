package com.king_sparkon_tracker.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.ContactInquiryRequest;
import com.king_sparkon_tracker.backend.dto.ContactInquiryResponse;
import com.king_sparkon_tracker.backend.model.ContactInquiry;
import com.king_sparkon_tracker.backend.repository.ContactInquiryRepository;

@Service
public class ContactInquiryService {

	private static final String SUCCESS_MESSAGE = "Thanks. We received your message and sent a confirmation email.";
	private static final String EMAIL_FAILURE_MESSAGE =
			"Your message was saved, but email delivery failed. Please try again or contact support.";

	private final ContactInquiryRepository contactInquiryRepository;
	private final AppEmailService appEmailService;
	private final String notificationEmail;

	public ContactInquiryService(
			ContactInquiryRepository contactInquiryRepository,
			AppEmailService appEmailService,
			@Value("${app.contact.notification-email}") String notificationEmail) {
		this.contactInquiryRepository = contactInquiryRepository;
		this.appEmailService = appEmailService;
		this.notificationEmail = notificationEmail;
	}

	@Transactional
	public ContactInquiryResponse submit(ContactInquiryRequest request) {
		ContactInquiry inquiry = contactInquiryRepository.save(new ContactInquiry(
				normalizeOptional(request.contactName()),
				normalizeRequired(request.businessName()),
				normalizeEmail(request.emailAddress()),
				normalizeOptional(request.phoneNumber()),
				normalizeRequired(request.message())));

		List<String> deliveryFailures = new ArrayList<>();
		boolean confirmationEmailSent = sendConfirmationEmail(inquiry, deliveryFailures);
		boolean notificationEmailSent = sendNotificationEmail(inquiry, deliveryFailures);

		if (confirmationEmailSent && notificationEmailSent) {
			inquiry.markEmailSent(true, true);
			return ContactInquiryResponse.from(contactInquiryRepository.save(inquiry), SUCCESS_MESSAGE);
		}

		inquiry.markEmailFailed(confirmationEmailSent, notificationEmailSent, String.join("; ", deliveryFailures));
		return ContactInquiryResponse.from(contactInquiryRepository.save(inquiry), EMAIL_FAILURE_MESSAGE);
	}

	private boolean sendConfirmationEmail(ContactInquiry inquiry, List<String> deliveryFailures) {
		try {
			boolean sent = appEmailService.sendContactInquiryConfirmationEmail(
					inquiry.getEmailAddress(),
					inquiry.getContactName(),
					inquiry.getBusinessName());
			if (!sent) {
				deliveryFailures.add("confirmation email was not sent");
			}
			return sent;
		} catch (RuntimeException exception) {
			deliveryFailures.add("confirmation email failed: " + exception.getMessage());
			return false;
		}
	}

	private boolean sendNotificationEmail(ContactInquiry inquiry, List<String> deliveryFailures) {
		try {
			boolean sent = appEmailService.sendContactInquiryNotificationEmail(
					notificationEmail,
					inquiry.getBusinessName(),
					inquiry.getContactName(),
					inquiry.getEmailAddress(),
					inquiry.getPhoneNumber(),
					inquiry.getMessage());
			if (!sent) {
				deliveryFailures.add("tracker company notification email was not sent");
			}
			return sent;
		} catch (RuntimeException exception) {
			deliveryFailures.add("tracker company notification email failed: " + exception.getMessage());
			return false;
		}
	}

	private String normalizeRequired(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeOptional(String value) {
		String normalized = normalizeRequired(value);
		return normalized.isBlank() ? null : normalized;
	}

	private String normalizeEmail(String email) {
		return normalizeRequired(email).toLowerCase(Locale.ROOT);
	}
}
