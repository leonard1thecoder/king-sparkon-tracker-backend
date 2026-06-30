package com.king_sparkon_tracker.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.model.ContactInquiry;
import com.king_sparkon_tracker.backend.repository.ContactInquiryRepository;

@Service
public class ContactInquiryEmailDispatcher {

	private static final Logger log = LoggerFactory.getLogger(ContactInquiryEmailDispatcher.class);

	private final ContactInquiryRepository contactInquiryRepository;
	private final AppEmailService appEmailService;
	private final String notificationEmail;

	public ContactInquiryEmailDispatcher(
			ContactInquiryRepository contactInquiryRepository,
			AppEmailService appEmailService,
			@Value("${app.contact.notification-email}") String notificationEmail) {
		this.contactInquiryRepository = contactInquiryRepository;
		this.appEmailService = appEmailService;
		this.notificationEmail = notificationEmail;
	}

	@Async("emailTaskExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void dispatchContactInquiryEmails(Long inquiryId) {
		ContactInquiry inquiry = contactInquiryRepository.findById(inquiryId)
				.orElse(null);
		if (inquiry == null) {
			log.warn("contact_inquiry_email_dispatch_skipped inquiryId={} reason=not_found", inquiryId);
			return;
		}

		List<String> deliveryFailures = new ArrayList<>();
		boolean confirmationEmailSent = sendConfirmationEmail(inquiry, deliveryFailures);
		boolean notificationEmailSent = sendNotificationEmail(inquiry, deliveryFailures);

		if (confirmationEmailSent && notificationEmailSent) {
			inquiry.markEmailSent(true, true);
			contactInquiryRepository.save(inquiry);
			log.info("contact_inquiry_email_dispatch_completed inquiryId={} confirmationSent=true notificationSent=true", inquiryId);
			return;
		}

		String failureReason = String.join("; ", deliveryFailures);
		inquiry.markEmailFailed(confirmationEmailSent, notificationEmailSent, failureReason);
		contactInquiryRepository.save(inquiry);
		log.warn("contact_inquiry_email_dispatch_failed inquiryId={} confirmationSent={} notificationSent={} reason={}",
				inquiryId, confirmationEmailSent, notificationEmailSent, failureReason);
	}

	private boolean sendConfirmationEmail(ContactInquiry inquiry, List<String> deliveryFailures) {
		try {
			boolean sent = appEmailService.sendContactInquiryConfirmationEmail(
					inquiry.getEmailAddress(),
					inquiry.getContactName(),
					inquiry.getBusinessName());
			if (!sent) {
				deliveryFailures.add("confirmation email was not sent to the customer");
			}
			return sent;
		} catch (RuntimeException exception) {
			deliveryFailures.add("confirmation email to customer failed: " + exception.getMessage());
			log.warn("contact_inquiry_confirmation_email_failed inquiryId={} recipient={} reason={}",
					inquiry.getId(), AppEmailService.maskEmail(inquiry.getEmailAddress()), exception.getMessage(), exception);
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
				deliveryFailures.add("King Sparkon Tracker support notification email was not sent");
			}
			return sent;
		} catch (RuntimeException exception) {
			deliveryFailures.add("King Sparkon Tracker support notification email failed: " + exception.getMessage());
			log.warn("contact_inquiry_notification_email_failed inquiryId={} recipient={} reason={}",
					inquiry.getId(), AppEmailService.maskEmail(notificationEmail), exception.getMessage(), exception);
			return false;
		}
	}
}
