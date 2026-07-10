package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.CreateRequest;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.CreateResponse;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.StatusResponse;
import com.king_sparkon_tracker.backend.service.EmbeddedCartPaymentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tuck-shop/cart-payments")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class EmbeddedCartPaymentController {

	private final EmbeddedCartPaymentService embeddedCartPaymentService;

	public EmbeddedCartPaymentController(EmbeddedCartPaymentService embeddedCartPaymentService) {
		this.embeddedCartPaymentService = embeddedCartPaymentService;
	}

	@PostMapping("/payment-intents")
	@ResponseStatus(HttpStatus.CREATED)
	public CreateResponse createPaymentIntent(@Valid @RequestBody CreateRequest request, Principal principal) {
		return embeddedCartPaymentService.create(request, principal.getName());
	}

	@GetMapping("/payment-intents/{paymentIntentId}")
	public StatusResponse paymentStatus(@PathVariable String paymentIntentId, Principal principal) {
		return embeddedCartPaymentService.status(paymentIntentId, principal.getName());
	}
}
