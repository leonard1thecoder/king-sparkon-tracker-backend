package com.king_sparkon_tracker.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.StripeWebhookResponse;
import com.king_sparkon_tracker.backend.model.StripeWebhookProcessingStatus;
import com.king_sparkon_tracker.backend.service.StripeWebhookService;

@RestController
@RequestMapping("/api/stripe/webhooks")
public class StripeWebhookController {

	private final StripeWebhookService stripeWebhookService;

	public StripeWebhookController(StripeWebhookService stripeWebhookService) {
		this.stripeWebhookService = stripeWebhookService;
	}

	@PostMapping
	public ResponseEntity<StripeWebhookResponse> process(
			@RequestBody String rawPayload,
			@RequestHeader(name = "Stripe-Signature", required = false) String stripeSignature) {
		StripeWebhookResponse response = stripeWebhookService.process(rawPayload, stripeSignature);
		if (response.status() == StripeWebhookProcessingStatus.SIGNATURE_FAILED) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (response.status() == StripeWebhookProcessingStatus.FAILED) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
		return ResponseEntity.ok(response);
	}
}
