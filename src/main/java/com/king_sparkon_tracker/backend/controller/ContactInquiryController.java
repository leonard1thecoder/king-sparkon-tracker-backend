package com.king_sparkon_tracker.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.ContactInquiryRequest;
import com.king_sparkon_tracker.backend.dto.ContactInquiryResponse;
import com.king_sparkon_tracker.backend.model.ContactInquiryStatus;
import com.king_sparkon_tracker.backend.service.ContactInquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contact-inquiries")
public class ContactInquiryController {

	private final ContactInquiryService contactInquiryService;

	public ContactInquiryController(ContactInquiryService contactInquiryService) {
		this.contactInquiryService = contactInquiryService;
	}

	@PostMapping
	public ResponseEntity<ContactInquiryResponse> submit(@Valid @RequestBody ContactInquiryRequest request) {
		ContactInquiryResponse response = contactInquiryService.submit(request);
		HttpStatus status = response.status() == ContactInquiryStatus.EMAIL_SENT
				? HttpStatus.CREATED
				: HttpStatus.ACCEPTED;
		return ResponseEntity.status(status).body(response);
	}
}
