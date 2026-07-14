package com.king_sparkon_tracker.backend.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, String> {
	List<GeneratedReport> findTop50ByBusiness_IdOrderByGeneratedAtDesc(Long businessId);
}
