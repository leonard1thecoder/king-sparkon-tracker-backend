package com.king_sparkon_tracker.backend.idempotency;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyRecordClaimService {

	private final IdempotencyRecordRepository repository;

	public IdempotencyRecordClaimService(IdempotencyRecordRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdempotencyService.Claim claimExisting(
			String key,
			String scope,
			String actorUsername,
			String requestHash,
			Instant expiresAt) {
		IdempotencyRecord existing = repository.findLocked(scope, actorUsername, key)
				.orElseThrow(() -> new IdempotencyConflictException("Idempotency record could not be claimed"));
		if (!existing.getRequestHash().equals(requestHash)) {
			throw new IdempotencyConflictException("Idempotency-Key was already used with a different request payload");
		}
		if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
			return new IdempotencyService.Claim(existing, IdempotencyService.ClaimDisposition.REPLAY);
		}
		if (existing.getStatus() == IdempotencyStatus.PROCESSING && existing.getExpiresAt().isAfter(Instant.now())) {
			throw new IdempotencyConflictException("An identical request is already being processed");
		}
		existing.restart(requestHash, expiresAt);
		return new IdempotencyService.Claim(repository.save(existing), IdempotencyService.ClaimDisposition.EXECUTE);
	}
}
