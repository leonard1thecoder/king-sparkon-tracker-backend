package com.king_sparkon_tracker.backend.outbox;

public enum OutboxStatus {
	PENDING,
	PROCESSING,
	PROCESSED,
	FAILED,
	DEAD_LETTER
}
