package com.king_sparkon_tracker.backend.finance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialJournalRepository extends JpaRepository<FinancialJournal, String> {

	Optional<FinancialJournal> findByBusiness_IdAndSourceTypeAndSourceReference(
			Long businessId,
			String sourceType,
			String sourceReference);

	@EntityGraph(attributePaths = "lines")
	List<FinancialJournal> findByBusiness_IdOrderByPostedAtAsc(Long businessId);
}
