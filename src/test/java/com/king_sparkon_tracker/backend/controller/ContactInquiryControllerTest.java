package com.king_sparkon_tracker.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.dto.ContactInquiryRequest;
import com.king_sparkon_tracker.backend.dto.ContactInquiryResponse;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.ContactInquiryStatus;
import com.king_sparkon_tracker.backend.service.ContactInquiryService;

class ContactInquiryControllerTest {

	private ContactInquiryService contactInquiryService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		contactInquiryService = mock(ContactInquiryService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ContactInquiryController(contactInquiryService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void submitReturnsCreatedWhenEmailsAreSent() throws Exception {
		when(contactInquiryService.submit(any(ContactInquiryRequest.class))).thenReturn(new ContactInquiryResponse(
				7L,
				ContactInquiryStatus.EMAIL_SENT,
				true,
				true,
				"Thanks. We received your message and sent a confirmation email.",
				LocalDateTime.parse("2026-06-17T10:00:00")));

		mockMvc.perform(post("/api/contact-inquiries")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "contactName": "Alice",
								  "businessName": "Alice Retail",
								  "emailAddress": "owner@example.com",
								  "phoneNumber": "+27 11 000 0000",
								  "message": "Need barcode tracking."
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.status").value("EMAIL_SENT"))
				.andExpect(jsonPath("$.confirmationEmailSent").value(true))
				.andExpect(jsonPath("$.notificationEmailSent").value(true));

		verify(contactInquiryService).submit(any(ContactInquiryRequest.class));
	}

	@Test
	void submitReturnsAcceptedWhenMessageIsSavedButEmailFails() throws Exception {
		when(contactInquiryService.submit(any(ContactInquiryRequest.class))).thenReturn(new ContactInquiryResponse(
				8L,
				ContactInquiryStatus.EMAIL_FAILED,
				true,
				false,
				"Your message was saved, but email delivery failed. Please try again or contact support.",
				LocalDateTime.parse("2026-06-17T10:00:00")));

		mockMvc.perform(post("/api/contact-inquiries")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "businessName": "Alice Retail",
								  "emailAddress": "owner@example.com",
								  "message": "Need barcode tracking."
								}
								"""))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.status").value("EMAIL_FAILED"))
				.andExpect(jsonPath("$.message").value("Your message was saved, but email delivery failed. Please try again or contact support."));
	}

	@Test
	void submitRejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/contact-inquiries")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "businessName": "Alice Retail",
								  "emailAddress": "not-an-email",
								  "message": "Need barcode tracking."
								}
								"""))
				.andExpect(status().isBadRequest());
	}
}
