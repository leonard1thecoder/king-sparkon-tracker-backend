package com.king_sparkon_tracker.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.PayPalWebhookResponse;
import com.king_sparkon_tracker.backend.service.PayPalWebhookService;

@RestController
@RequestMapping("/api/paypal/webhooks")
public class PayPalWebhookController {

	private final PayPalWebhookService payPalWebhookService;

	public PayPalWebhookController(PayPalWebhookService payPalWebhookService) {
		this.payPalWebhookService = payPalWebhookService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public PayPalWebhookResponse handleWebhook(
			@RequestBody String rawPayload,
			@RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
			@RequestHeader("PAYPAL-TRANSMISSION-TIME") String transmissionTime,
			@RequestHeader("PAYPAL-CERT-URL") String certUrl,
			@RequestHeader("PAYPAL-AUTH-ALGO") String authAlgo,
			@RequestHeader("PAYPAL-TRANSMISSION-SIG") String transmissionSignature) {
		return payPalWebhookService.process(
				rawPayload,
				transmissionId,
				transmissionTime,
				certUrl,
				authAlgo,
				transmissionSignature
		);
	}
}
