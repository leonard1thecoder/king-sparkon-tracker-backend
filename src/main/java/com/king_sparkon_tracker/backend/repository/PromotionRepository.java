package com.king_sparkon_tracker.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.Promotion;
import com.king_sparkon_tracker.backend.model.PromotionOrigin;
import com.king_sparkon_tracker.backend.model.PromotionStatus;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

	Page<Promotion> findByBusiness_IdOrderByCreatedDateDesc(Long businessId, Pageable pageable);

	List<Promotion> findTop20ByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(PromotionStatus status, OffsetDateTime now);

	boolean existsByOriginAndCreatedDateAfter(PromotionOrigin origin, OffsetDateTime createdAfter);
}
