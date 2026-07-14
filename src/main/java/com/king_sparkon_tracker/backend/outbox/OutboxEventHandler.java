package com.king_sparkon_tracker.backend.outbox;

public interface OutboxEventHandler {
	OutboxEventType supports();
	void handle(OutboxEvent event) throws Exception;
}
