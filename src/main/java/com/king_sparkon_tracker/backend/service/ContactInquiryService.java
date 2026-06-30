package com.king_sparkon_tracker.backend.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.king_sparkon_tracker.backend.dto.ContactInquiryRequest;
import com.king_sparkon_tracker.backend.dto.ContactInquiryResponse;
import com.king_sparkon_tracker.backend.model.ContactInquiry;
import com.king_sparkon_tracker.backend.repository.ContactInquiryRepository;

@Service
public class ContactInquiryService {

	private static final String QUEUED_MESSAGE =
			"Thanks. We received your message and queued your confirmation email. Our team has also been notified.";

	private final ContactInquiryRepository contactInquiryRepository;
	private final ContactInquiryEmailDispatcher emailDispatcher;

	public ContactInquiryService(
			ContactInquiryRepository contactInquiryRepository,
			ContactInquiryEmailDispatcher emailDispatcher) {
		this.contactInquiryRepository = contactInquiryRepository;
		this.emailDispatcher = emailDispatcher;
	}

	@Transactional
	public ContactInquiryResponse submit(ContactInquiryRequest request) {
		ContactInquiry inquiry = new ContactInquiry(
				normalizeOptional(request.contactName()),
				normalizeRequired(request.businessName()),
				normalizeEmail(request.emailAddress()),
				normalizeOptional(request.phoneNumber()),
				normalizeRequired(request.message()));
		inquiry.markEmailQueued();
		ContactInquiry savedInquiry = contactInquiryRepository.save(inquiry);
		queueEmailDispatchAfterCommit(savedInquiry.getId());
		return ContactInquiryResponse.from(savedInquiry, QUEUED_MESSAGE);
	}

	private void queueEmailDispatchAfterCommit(Long inquiryId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			emailDispatcher.dispatchContactInquiryEmails(inquiryId);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				emailDispatcher.dispatchContactInquiryEmails(inquiryId);
			}
		});
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
