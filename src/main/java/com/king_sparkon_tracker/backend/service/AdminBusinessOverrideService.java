package com.king_sparkon_tracker.backend.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.AdminBusinessOverrideRequest;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.BusinessSubscriptionRepository;

@Service
@Transactional
public class AdminBusinessOverrideService {

	private static final Logger log = LoggerFactory.getLogger(AdminBusinessOverrideService.class);

	private final BusinessRepository businessRepository;
	private final BusinessSubscriptionRepository subscriptionRepository;
	private final BillingAuditService billingAuditService;
	private final Clock clock;
	private final AppEmailService appEmailService;

	public AdminBusinessOverrideService(
			BusinessRepository businessRepository,
			BusinessSubscriptionRepository subscriptionRepository,
			BillingAuditService billingAuditService,
			Clock clock,
			AppEmailService appEmailService) {
		this.businessRepository = businessRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.billingAuditService = billingAuditService;
		this.clock = clock;
		this.appEmailService = appEmailService;
	}

	public BusinessBillingResponse overrideBusiness(
			Long businessId,
			AdminBusinessOverrideRequest request,
			String actorUsername) {
		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));

		switch (request.action()) {
			case REACTIVATE -> reactivate(business, request, actorUsername);
			case DEACTIVATE -> deactivate(business, request, actorUsername);
			case CANCEL -> cancel(business, request, actorUsername);
			case MARK_PAST_DUE -> markPastDue(business, request, actorUsername);
		}

		Business savedBusiness = businessRepository.save(business);
		BusinessSubscription subscription = subscriptionRepository.findTopByBusiness_IdOrderByCreatedDateDesc(savedBusiness.getId())
				.orElse(null);

		sendAdminBusinessStatusChangedNotification(savedBusiness, request, actorUsername);

		return BusinessBillingResponse.from(savedBusiness, subscription);
	}

	private void reactivate(Business business, AdminBusinessOverrideRequest request, String actorUsername) {
		BusinessPlan plan = request.businessPlan() == null ? BusinessPlan.PLUS : request.businessPlan();
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime periodEnd = request.currentBillingPeriodEndDate() == null
				? now.plusMonths(1)
				: request.currentBillingPeriodEndDate();

		business.setBusinessPlan(plan);
		business.setBusinessStatus(BusinessStatus.ACTIVE);
		business.setCurrentBillingPeriodStartDate(now);
		business.setCurrentBillingPeriodEndDate(periodEnd);
		business.setLastPaymentDate(now);
		business.setNextBillingDate(periodEnd);

		billingAuditService.record(
				business,
				BillingAuditAction.ADMIN_OVERRIDE_ACTIVATED,
				actorUsername,
				null,
				business.getPaypalSubscriptionId(),
				message("Admin reactivated business", request.reason())
		);
	}

	private void deactivate(Business business, AdminBusinessOverrideRequest request, String actorUsername) {
		business.deactivate();

		billingAuditService.record(
				business,
				BillingAuditAction.ADMIN_OVERRIDE_DEACTIVATED,
				actorUsername,
				null,
				business.getPaypalSubscriptionId(),
				message("Admin deactivated business", request.reason())
		);
	}

	private void cancel(Business business, AdminBusinessOverrideRequest request, String actorUsername) {
		business.cancel();

		billingAuditService.record(
				business,
				BillingAuditAction.ADMIN_OVERRIDE_DEACTIVATED,
				actorUsername,
				null,
				business.getPaypalSubscriptionId(),
				message("Admin cancelled business", request.reason())
		);
	}

	private void markPastDue(Business business, AdminBusinessOverrideRequest request, String actorUsername) {
		business.markPastDue();

		billingAuditService.record(
				business,
				BillingAuditAction.PAYMENT_FAILED,
				actorUsername,
				null,
				business.getPaypalSubscriptionId(),
				message("Admin marked business past due", request.reason())
		);
	}

	private String message(String base, String reason) {
		if (reason == null || reason.isBlank()) {
			return base;
		}

		return base + ". Reason: " + reason;
	}

	private void sendAdminBusinessStatusChangedNotification(
			Business business,
			AdminBusinessOverrideRequest request,
			String actorUsername) {
		try {
			appEmailService.sendAdminBusinessStatusChangedEmail(
					business,
					request.action(),
					request.reason(),
					actorUsername);
		} catch (RuntimeException exception) {
			log.warn(
					"admin_business_status_changed_email_failed_non_blocking recipient={} businessId={} action={} actor={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					request.action(),
					actorUsername,
					exception.getMessage());
		}
	}
}
