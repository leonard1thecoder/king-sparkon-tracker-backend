package com.king_sparkon_tracker.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.BillingAuditLogResponse;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.BillingAuditLog;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.repository.BillingAuditLogRepository;

@Service
@Transactional
public class BillingAuditService {

	private final BillingAuditLogRepository billingAuditLogRepository;

	public BillingAuditService(BillingAuditLogRepository billingAuditLogRepository) {
		this.billingAuditLogRepository = billingAuditLogRepository;
	}

	public void record(
			Business business,
			BillingAuditAction action,
			String actorUsername,
			String paypalEventId,
			String paypalSubscriptionId,
			String message) {
		billingAuditLogRepository.save(new BillingAuditLog(
				business,
				action,
				actorUsername,
				paypalEventId,
				paypalSubscriptionId,
				message
		));
	}

	@Transactional(readOnly = true)
	public List<BillingAuditLogResponse> logsForBusiness(Long businessId) {
		return billingAuditLogRepository.findByBusiness_IdOrderByCreatedDateDesc(businessId)
				.stream()
				.map(BillingAuditLogResponse::from)
				.toList();
	}
}
