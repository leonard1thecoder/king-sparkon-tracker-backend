package com.king_sparkon_tracker.backend.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OutboxEventTest {

	@Test
	void successfulDeliveryBecomesTerminalAndClearsLock() {
		OutboxEvent event = new OutboxEvent(
				"EMAIL", "42", OutboxEventType.EMAIL_SEND, "{}", "email:42", Instant.now());

		event.claim();
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
		assertThat(event.getLockedAt()).isNotNull();

		event.processed();
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
		assertThat(event.getProcessedAt()).isNotNull();
		assertThat(event.getLockedAt()).isNull();
	}

	@Test
	void repeatedFailuresMoveEventToDeadLetterAfterBoundedAttempts() {
		OutboxEvent event = new OutboxEvent(
				"REPORT", "9", OutboxEventType.REPORT_GENERATION, "{}", "report:9", Instant.now());

		for (int attempt = 0; attempt < 10; attempt++) {
			event.claim();
			event.retry(new IllegalStateException("provider unavailable"));
		}

		assertThat(event.getAttempts()).isEqualTo(10);
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
		assertThat(event.getLastError()).contains("provider unavailable");
	}
}
