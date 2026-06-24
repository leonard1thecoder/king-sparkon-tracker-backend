package com.king_sparkon_tracker.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.SubscribeRequest;
import com.king_sparkon_tracker.backend.dto.SubscriberResponse;
import com.king_sparkon_tracker.backend.service.SubscriberService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subscribers")
public class SubscriberController {

	private final SubscriberService subscriberService;

	public SubscriberController(SubscriberService subscriberService) {
		this.subscriberService = subscriberService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SubscriberResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
		return SubscriberResponse.from(subscriberService.subscribe(request));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribe(@RequestParam String contact) {
		subscriberService.unsubscribe(contact);
	}
}
