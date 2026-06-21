package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.model.AuditLog;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.repository.AuditLogRepository;

@Service
public class AuditLogService {

	private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	/**
	 * Persists a business audit event with the caller transaction so business foreign keys stay consistent.
	 */
	@Transactional
	public AuditLog record(String action, String entityType, String entityId, String actorUsername, String details) {
		return record(action, entityType, entityId, actorUsername, details, null);
	}

	/**
	 * Persists a business-scoped audit event with the caller transaction.
	 */
	@Transactional
	public AuditLog record(
			String action,
			String entityType,
			String entityId,
			String actorUsername,
			String details,
			Business business) {
		String actor = StringUtils.hasText(actorUsername) ? actorUsername : "system";
		AuditLog savedLog = auditLogRepository.save(new AuditLog(action, entityType, entityId, actor, details, business));
		log.info(
				"audit_log_recorded action={} entityType={} entityId={} businessId={} actor={}",
				action,
				entityType,
				entityId,
				business == null ? null : business.getId(),
				actor);
		return savedLog;
	}

	/**
	 * Returns audit entries newest-first for owner review screens.
	 */
	@Transactional(readOnly = true)
	public Page<AuditLog> listAuditLogs(Pageable pageable) {
		log.debug("audit_logs_list_requested page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
		return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
	}

	/**
	 * Returns audit entries newest-first for one business.
	 */
	@Transactional(readOnly = true)
	public Page<AuditLog> listAuditLogs(Long businessId, Pageable pageable) {
		log.debug(
				"audit_logs_list_requested businessId={} page={} size={}",
				businessId,
				pageable.getPageNumber(),
				pageable.getPageSize());
		return auditLogRepository.findByBusiness_IdOrderByCreatedAtDesc(businessId, pageable);
	}
}
