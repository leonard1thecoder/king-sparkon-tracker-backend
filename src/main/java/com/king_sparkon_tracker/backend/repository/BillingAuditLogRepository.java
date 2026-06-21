package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.BillingAuditLog;

public interface BillingAuditLogRepository extends JpaRepository<BillingAuditLog, Long> {

	List<BillingAuditLog> findByBusiness_IdOrderByCreatedDateDesc(Long businessId);
}
