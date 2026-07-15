package com.king_sparkon_tracker.backend.outbox;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventInsertService {

	private final OutboxEventRepository repository;

	public OutboxEventInsertService(OutboxEventRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public OutboxEvent insert(
			String aggregateType,
			String aggregateId,
			OutboxEventType eventType,
			String payload,
			String deduplicationKey) {
		return repository.saveAndFlush(new OutboxEvent(
				aggregateType,
				aggregateId,
				eventType,
				payload,
				deduplicationKey,
				Instant.now()));
	}
}
