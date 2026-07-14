package com.king_sparkon_tracker.backend.idempotency;

public class IdempotencyConflictException extends RuntimeException {
	public IdempotencyConflictException(String message) {
		super(message);
	}
}
