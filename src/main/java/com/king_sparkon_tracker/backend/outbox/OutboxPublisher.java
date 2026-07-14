package com.king_sparkon_tracker.backend.outbox;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxPublisher {

	private final OutboxEventRepository repository;
	private final ObjectMapper objectMapper;
	private final OutboxEventInsertService insertService;

	public OutboxPublisher(
			OutboxEventRepository repository,
			ObjectMapper objectMapper,
			OutboxEventInsertService insertService) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.insertService = insertService;
	}

	public OutboxEvent publish(
			String aggregateType,
			String aggregateId,
			OutboxEventType eventType,
			Object payload,
			String deduplicationKey) {
		return repository.findByDeduplicationKey(deduplicationKey)
				.orElseGet(() -> insert(aggregateType, aggregateId, eventType, payload, deduplicationKey));
	}

	public OutboxEvent email(String aggregateType, String aggregateId, OutboxPayloads.Email email, String deduplicationKey) {
		return publish(aggregateType, aggregateId, OutboxEventType.EMAIL_SEND, email, deduplicationKey);
	}

	public OutboxEvent notification(String aggregateType, String aggregateId, String eventName, Map<String, Object> attributes) {
		String deduplicationKey = "notification:" + eventName + ":" + aggregateType + ":" + aggregateId;
		return publish(
				aggregateType,
				aggregateId,
				OutboxEventType.NOTIFICATION_LOG,
				new OutboxPayloads.Notification(eventName, attributes),
				deduplicationKey);
	}

	private OutboxEvent insert(
			String aggregateType,
			String aggregateId,
			OutboxEventType eventType,
			Object payload,
			String deduplicationKey) {
		try {
			return insertService.insert(aggregateType, aggregateId, eventType, json(payload), deduplicationKey);
		} catch (DataIntegrityViolationException duplicate) {
			return repository.findByDeduplicationKey(deduplicationKey).orElseThrow(() -> duplicate);
		}
	}

	private String json(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Outbox payload is not serializable", exception);
		}
	}
}
