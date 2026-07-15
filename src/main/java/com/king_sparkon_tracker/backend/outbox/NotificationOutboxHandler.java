package com.king_sparkon_tracker.backend.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationOutboxHandler implements OutboxEventHandler {

	private static final Logger log = LoggerFactory.getLogger(NotificationOutboxHandler.class);
	private final ObjectMapper objectMapper;

	public NotificationOutboxHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public OutboxEventType supports() { return OutboxEventType.NOTIFICATION_LOG; }

	@Override
	public void handle(OutboxEvent event) throws Exception {
		OutboxPayloads.Notification payload = objectMapper.readValue(event.getPayload(), OutboxPayloads.Notification.class);
		log.info("notification_dispatched event={} aggregateType={} aggregateId={} attributes={}",
				payload.eventName(), event.getAggregateType(), event.getAggregateId(), payload.attributes());
	}
}
