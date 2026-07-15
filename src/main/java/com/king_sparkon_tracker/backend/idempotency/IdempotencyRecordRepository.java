package com.king_sparkon_tracker.backend.idempotency;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

	Optional<IdempotencyRecord> findByRequestScopeAndActorUsernameAndIdempotencyKey(
			String requestScope,
			String actorUsername,
			String idempotencyKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select record from IdempotencyRecord record
			where record.requestScope = :scope
			and record.actorUsername = :actorUsername
			and record.idempotencyKey = :idempotencyKey
			""")
	Optional<IdempotencyRecord> findLocked(
			@Param("scope") String scope,
			@Param("actorUsername") String actorUsername,
			@Param("idempotencyKey") String idempotencyKey);

	List<IdempotencyRecord> findTop200ByExpiresAtBeforeOrderByExpiresAtAsc(Instant cutoff);
}
