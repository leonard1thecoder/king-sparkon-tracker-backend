package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.ContactInquiryRequest;
import com.king_sparkon_tracker.backend.dto.ContactInquiryResponse;
import com.king_sparkon_tracker.backend.model.ContactInquiry;
import com.king_sparkon_tracker.backend.model.ContactInquiryStatus;
import com.king_sparkon_tracker.backend.repository.ContactInquiryRepository;

@ExtendWith(MockitoExtension.class)
class ContactInquiryServiceTest {

	@Mock
	private ContactInquiryRepository contactInquiryRepository;

	@Mock
	private AppEmailService appEmailService;

	private ContactInquiryService contactInquiryService;

	@BeforeEach
	void setUp() {
		contactInquiryService = new ContactInquiryService(
				contactInquiryRepository,
				appEmailService,
				"sales@king-sparkon-tracker.com");

		when(contactInquiryRepository.save(any(ContactInquiry.class))).thenAnswer(invocation -> {
			ContactInquiry inquiry = invocation.getArgument(0);
			if (inquiry.getId() == null) {
				ReflectionTestUtils.setField(inquiry, "id", 42L);
			}
			if (inquiry.getCreatedAt() == null) {
				ReflectionTestUtils.setField(inquiry, "createdAt", LocalDateTime.parse("2026-06-17T10:00:00"));
			}
			ReflectionTestUtils.setField(inquiry, "updatedAt", LocalDateTime.parse("2026-06-17T10:00:00"));
			return inquiry;
		});
	}

	@Test
	void submitStoresInquiryAndMarksEmailSentWhenBothEmailsSend() {
		when(appEmailService.sendContactInquiryConfirmationEmail(
				eq("owner@example.com"),
				eq("Alice"),
				eq("Alice Retail")))
				.thenReturn(true);
		when(appEmailService.sendContactInquiryNotificationEmail(
				eq("sales@king-sparkon-tracker.com"),
				eq("Alice Retail"),
				eq("Alice"),
				eq("owner@example.com"),
				eq("+27 11 000 0000"),
				eq("Need barcode tracking for alcohol and non alcohol products.")))
				.thenReturn(true);

		ContactInquiryResponse response = contactInquiryService.submit(new ContactInquiryRequest(
				" Alice ",
				" Alice Retail ",
				" OWNER@EXAMPLE.COM ",
				" +27 11 000 0000 ",
				" Need barcode tracking for alcohol and non alcohol products. "));

		assertThat(response.id()).isEqualTo(42L);
		assertThat(response.status()).isEqualTo(ContactInquiryStatus.EMAIL_SENT);
		assertThat(response.confirmationEmailSent()).isTrue();
		assertThat(response.notificationEmailSent()).isTrue();
		assertThat(response.message()).isEqualTo("Thanks. We received your message and sent a confirmation email.");

		ArgumentCaptor<ContactInquiry> inquiryCaptor = ArgumentCaptor.forClass(ContactInquiry.class);
		verify(contactInquiryRepository, org.mockito.Mockito.times(2)).save(inquiryCaptor.capture());

		ContactInquiry storedInquiry = inquiryCaptor.getAllValues().get(1);
		assertThat(storedInquiry.getBusinessName()).isEqualTo("Alice Retail");
		assertThat(storedInquiry.getEmailAddress()).isEqualTo("owner@example.com");
		assertThat(storedInquiry.getMessage()).isEqualTo("Need barcode tracking for alcohol and non alcohol products.");
		assertThat(storedInquiry.getFailureReason()).isNull();
	}

	@Test
	void submitStoresInquiryAndMarksEmailFailedWhenNotificationFails() {
		when(appEmailService.sendContactInquiryConfirmationEmail(
				eq("owner@example.com"),
				isNull(),
				eq("Alice Retail")))
				.thenReturn(true);
		when(appEmailService.sendContactInquiryNotificationEmail(
				eq("sales@king-sparkon-tracker.com"),
				eq("Alice Retail"),
				isNull(),
				eq("owner@example.com"),
				isNull(),
				eq("Please call me.")))
				.thenReturn(false);

		ContactInquiryResponse response = contactInquiryService.submit(new ContactInquiryRequest(
				null,
				"Alice Retail",
				"owner@example.com",
				null,
				"Please call me."));

		assertThat(response.status()).isEqualTo(ContactInquiryStatus.EMAIL_FAILED);
		assertThat(response.confirmationEmailSent()).isTrue();
		assertThat(response.notificationEmailSent()).isFalse();
		assertThat(response.message()).isEqualTo("Your message was saved, but email delivery failed. Please try again or contact support.");

		ArgumentCaptor<ContactInquiry> inquiryCaptor = ArgumentCaptor.forClass(ContactInquiry.class);
		verify(contactInquiryRepository, org.mockito.Mockito.times(2)).save(inquiryCaptor.capture());

		ContactInquiry storedInquiry = inquiryCaptor.getAllValues().get(1);
		assertThat(storedInquiry.getStatus()).isEqualTo(ContactInquiryStatus.EMAIL_FAILED);
		assertThat(storedInquiry.getFailureReason()).contains("tracker company notification email was not sent");
	}
}
