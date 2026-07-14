package com.king_sparkon_tracker.backend.outbox;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

	Optional<OutboxEvent> findByDeduplicationKey(String deduplicationKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select event from OutboxEvent event
			where event.status in :statuses
			and event.availableAt <= :now
			order by event.createdAt asc
			""")
	List<OutboxEvent> lockDispatchable(
			@Param("statuses") Collection<OutboxStatus> statuses,
			@Param("now") Instant now,
			Pageable pageable);

	List<OutboxEvent> findTop100ByStatusAndLockedAtBeforeOrderByLockedAtAsc(OutboxStatus status, Instant cutoff);
}
