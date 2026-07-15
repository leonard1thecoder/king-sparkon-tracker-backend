package com.king_sparkon_tracker.backend.outbox;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EmailOutboxHandler implements OutboxEventHandler {

	private final ObjectMapper objectMapper;
	private final EmailDeliveryService deliveryService;

	public EmailOutboxHandler(ObjectMapper objectMapper, EmailDeliveryService deliveryService) {
		this.objectMapper = objectMapper;
		this.deliveryService = deliveryService;
	}

	@Override
	public OutboxEventType supports() { return OutboxEventType.EMAIL_SEND; }

	@Override
	public void handle(OutboxEvent event) throws Exception {
		deliveryService.deliver(objectMapper.readValue(event.getPayload(), OutboxPayloads.Email.class));
	}
}
