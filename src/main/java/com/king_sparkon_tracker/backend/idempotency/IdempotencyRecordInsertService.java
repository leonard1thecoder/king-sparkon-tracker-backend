package com.king_sparkon_tracker.backend.idempotency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyRecordInsertService {

	private final IdempotencyRecordRepository repository;

	public IdempotencyRecordInsertService(IdempotencyRecordRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdempotencyRecord insert(IdempotencyRecord record) {
		return repository.saveAndFlush(record);
	}
}
