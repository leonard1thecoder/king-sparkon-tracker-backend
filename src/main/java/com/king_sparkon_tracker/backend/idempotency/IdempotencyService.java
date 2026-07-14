package com.king_sparkon_tracker.backend.idempotency;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

	public enum ClaimDisposition { EXECUTE, REPLAY }

	public record Claim(IdempotencyRecord record, ClaimDisposition disposition) {
		public boolean replay() { return disposition == ClaimDisposition.REPLAY; }
	}

	private final IdempotencyRecordRepository repository;
	private final IdempotencyRecordInsertService insertService;
	private final IdempotencyRecordClaimService claimService;

	public IdempotencyService(
			IdempotencyRecordRepository repository,
			IdempotencyRecordInsertService insertService,
			IdempotencyRecordClaimService claimService) {
		this.repository = repository;
		this.insertService = insertService;
		this.claimService = claimService;
	}

	public Claim claim(
			String key,
			String scope,
			String actorUsername,
			Long businessId,
			String requestHash,
			long ttlSeconds) {
		Instant expiresAt = Instant.now().plusSeconds(Math.max(60L, ttlSeconds));
		try {
			IdempotencyRecord created = insertService.insert(new IdempotencyRecord(
					key, scope, actorUsername, businessId, requestHash, expiresAt));
			return new Claim(created, ClaimDisposition.EXECUTE);
		} catch (DataIntegrityViolationException duplicate) {
			return claimService.claimExisting(key, scope, actorUsername, requestHash, expiresAt);
		}
	}

	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	public void complete(Long recordId, String responseBody, String responseType, int httpStatus) {
		IdempotencyRecord record = repository.findById(recordId)
				.orElseThrow(() -> new IdempotencyConflictException("Idempotency record was lost before completion"));
		record.complete(responseBody, responseType, httpStatus);
		repository.save(record);
	}

	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	public void fail(Long recordId, Throwable throwable) {
		repository.findById(recordId).ifPresent(record -> {
			record.fail(throwable == null ? "Request failed" : throwable.getMessage());
			repository.save(record);
		});
	}

	@Scheduled(cron = "0 20 2 * * *", zone = "Africa/Johannesburg")
	@Transactional
	public void purgeExpiredRecords() {
		repository.deleteAllInBatch(repository.findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(Instant.now().minusSeconds(86_400L)));
	}
}
