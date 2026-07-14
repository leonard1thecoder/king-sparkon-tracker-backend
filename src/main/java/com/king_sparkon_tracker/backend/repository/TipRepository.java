package com.king_sparkon_tracker.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;

public interface TipRepository extends JpaRepository<Tip, Long> {

	List<Tip> findByWorker_IdOrderByCreatedDesc(Long workerId);

	List<Tip> findByWorker_Business_IdOrderByCreatedDesc(Long businessId);

	List<Tip> findByWorker_Business_IdAndStatusOrderByCreatedDesc(Long businessId, TipStatus status);

	List<Tip> findByWithdrawal_IdOrderByCreatedDesc(Long withdrawalId);

	List<Tip> findByStatus(TipStatus status);

	List<Tip> findByStatusOrderByCreatedDesc(TipStatus status);

	List<Tip> findByWorker_IdAndStatusAndWithdrawalIsNullAndCreatedLessThanEqualOrderByCreatedDesc(
			Long workerId,
			TipStatus status,
			OffsetDateTime created);
}
