package com.king_sparkon_tracker.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

	Page<AuditLog> findByBusiness_IdOrderByCreatedAtDesc(Long businessId, Pageable pageable);
}
