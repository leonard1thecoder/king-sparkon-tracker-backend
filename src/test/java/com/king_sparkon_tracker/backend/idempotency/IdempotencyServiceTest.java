package com.king_sparkon_tracker.backend.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

	@Mock
	private IdempotencyRecordRepository repository;
	@Mock
	private IdempotencyRecordInsertService insertService;
	@Mock
	private IdempotencyRecordClaimService claimService;

	private IdempotencyService service;

	@BeforeEach
	void setUp() {
		service = new IdempotencyService(repository, insertService, claimService);
	}

	@Test
	void newKeyClaimsExecution() {
		IdempotencyRecord record = record("key-1", "hash-1");
		when(insertService.insert(any(IdempotencyRecord.class))).thenReturn(record);

		IdempotencyService.Claim claim = service.claim(
				"key-1", "transaction-create", "owner", 7L, "hash-1", 3600L);

		assertThat(claim.replay()).isFalse();
		assertThat(claim.record()).isSameAs(record);
	}

	@Test
	void duplicateKeyDelegatesToLockedClaimForReplayOrConflict() {
		IdempotencyRecord completed = record("key-1", "hash-1");
		completed.complete("{\"id\":42}", "response", 201);
		when(insertService.insert(any(IdempotencyRecord.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));
		when(claimService.claimExisting(eq("key-1"), eq("transaction-create"), eq("owner"), eq("hash-1"), any(Instant.class)))
				.thenReturn(new IdempotencyService.Claim(completed, IdempotencyService.ClaimDisposition.REPLAY));

		IdempotencyService.Claim claim = service.claim(
				"key-1", "transaction-create", "owner", 7L, "hash-1", 3600L);

		assertThat(claim.replay()).isTrue();
		assertThat(claim.record().getHttpStatus()).isEqualTo(201);
		verify(claimService).claimExisting(eq("key-1"), eq("transaction-create"), eq("owner"), eq("hash-1"), any(Instant.class));
	}

	private IdempotencyRecord record(String key, String hash) {
		return new IdempotencyRecord(key, "transaction-create", "owner", 7L, hash, Instant.now().plusSeconds(3600));
	}
}
