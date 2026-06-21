package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.BillingDashboardResponse;
import com.king_sparkon_tracker.backend.dto.BillingPlanResponse;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.dto.CreateBusinessSubscriptionRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.BusinessSubscriptionRepository;

@Service
@Transactional
public class BusinessBillingService {

	private final BusinessRepository businessRepository;
	private final BusinessSubscriptionRepository subscriptionRepository;
	private final TrackerUserRepository userRepository;
	private final BusinessPlanPolicyService businessPlanPolicyService;
	private final PayPalBillingClient payPalBillingClient;
	private final BillingAuditService billingAuditService;
	private final Clock clock;

	public BusinessBillingService(
			BusinessRepository businessRepository,
			BusinessSubscriptionRepository subscriptionRepository,
			TrackerUserRepository userRepository,
			BusinessPlanPolicyService businessPlanPolicyService,
			PayPalBillingClient payPalBillingClient,
			BillingAuditService billingAuditService,
			Clock clock) {
		this.businessRepository = businessRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.userRepository = userRepository;
		this.businessPlanPolicyService = businessPlanPolicyService;
		this.payPalBillingClient = payPalBillingClient;
		this.billingAuditService = billingAuditService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<BillingPlanResponse> plans() {
		return businessPlanPolicyService.billingPlans();
	}

	@Transactional(readOnly = true)
	public BusinessBillingResponse currentBilling(String actorUsername) {
		Business business = businessForOwner(actorUsername);
		BusinessSubscription subscription = latestSubscription(business);
		return BusinessBillingResponse.from(business, subscription);
	}

	@Transactional(readOnly = true)
	public BillingDashboardResponse dashboard(String actorUsername) {
		Business business = businessForActor(actorUsername);
		BusinessSubscription subscription = latestSubscription(business);
		LocalDateTime now = LocalDateTime.now(clock);

		long trialDaysLeft = 0;
		if (business.getBusinessStatus() == BusinessStatus.TRIAL && business.getTrialEndDate() != null) {
			trialDaysLeft = Math.max(java.time.Duration.between(now, business.getTrialEndDate()).toDays(), 0);
		}

		return BillingDashboardResponse.from(
				business,
				subscription,
				trialDaysLeft,
				businessPlanPolicyService.isFeatureEnabled(business, BusinessFeature.CREATE_PRODUCTS),
				businessPlanPolicyService.isFeatureEnabled(business, BusinessFeature.SCAN_BARCODES),
				businessPlanPolicyService.isFeatureEnabled(business, BusinessFeature.WORKER_TIPS_PLATFORM),
				businessPlanPolicyService.isFeatureEnabled(business, BusinessFeature.BUSINESS_ANALYSIS_AI),
				businessPlanPolicyService.isFeatureEnabled(business, BusinessFeature.WORKER_CLOCKER),
				businessPlanPolicyService.billingPlans()
		);
	}

	public BusinessBillingResponse createSubscription(String actorUsername, CreateBusinessSubscriptionRequest request) {
		Business business = businessForOwner(actorUsername);

		if (request.plan() == BusinessPlan.FREE_TRIAL) {
			throw new IllegalArgumentException("FREE_TRIAL cannot be purchased");
		}

		validateRequest(request);

		BigDecimal amount = amountFor(request.plan(), request.billingInterval(), request.termYears());

		BusinessSubscription subscription = new BusinessSubscription(
				business,
				request.plan(),
				request.billingInterval(),
				normalizedTermYears(request),
				amount,
				"ZAR"
		);

		subscription = subscriptionRepository.save(subscription);

		PayPalBillingClient.CreatedPayPalSubscription paypalSubscription = payPalBillingClient.createSubscription(
				request.plan(),
				request.billingInterval(),
				normalizedTermYears(request)
		);

		subscription.markApprovalPending(
				paypalSubscription.paypalSubscriptionId(),
				paypalSubscription.paypalSubscriptionToken(),
				paypalSubscription.paypalPlanId(),
				paypalSubscription.approvalUrl()
		);

		BusinessSubscription savedSubscription = subscriptionRepository.save(subscription);

		billingAuditService.record(
				business,
				BillingAuditAction.PLAN_APPROVAL_PENDING,
				actorUsername,
				null,
				savedSubscription.getPaypalSubscriptionId(),
				"PayPal approval pending for " + savedSubscription.getBusinessPlan()
		);

		return BusinessBillingResponse.from(business, savedSubscription);
	}

	public BusinessBillingResponse activateApprovedSubscription(String actorUsername, Long subscriptionId) {
		Business business = businessForOwner(actorUsername);

		BusinessSubscription subscription = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Business subscription not found: " + subscriptionId));

		if (!subscription.getBusiness().getId().equals(business.getId())) {
			throw new IllegalArgumentException("Subscription does not belong to this business");
		}

		String paypalStatus = payPalBillingClient.subscriptionStatus(subscription.getPaypalSubscriptionId());

		if (!"ACTIVE".equalsIgnoreCase(paypalStatus) && !"APPROVAL_PENDING".equalsIgnoreCase(paypalStatus)) {
			throw new IllegalArgumentException("PayPal subscription is not approved. Current status: " + paypalStatus);
		}

		activateSubscription(subscription, actorUsername, null, "Manual approval activation");

		return BusinessBillingResponse.from(business, subscription);
	}

	public void handlePayPalSubscriptionActivated(String paypalSubscriptionId, String paypalEventId) {
		BusinessSubscription subscription = subscriptionByPayPalId(paypalSubscriptionId);
		activateSubscription(subscription, "paypal-webhook", paypalEventId, "PayPal subscription activated");
	}

	public void handlePayPalPaymentCompleted(String paypalSubscriptionId, String paypalEventId) {
		BusinessSubscription subscription = subscriptionByPayPalId(paypalSubscriptionId);
		activateSubscription(subscription, "paypal-webhook", paypalEventId, "PayPal subscription payment completed");

		billingAuditService.record(
				subscription.getBusiness(),
				BillingAuditAction.PAYMENT_SUCCESS,
				"paypal-webhook",
				paypalEventId,
				paypalSubscriptionId,
				"PayPal subscription payment completed"
		);
	}

	public void handlePayPalPaymentFailed(String paypalSubscriptionId, String paypalEventId) {
		BusinessSubscription subscription = subscriptionByPayPalId(paypalSubscriptionId);
		Business business = subscription.getBusiness();

		subscription.markPaymentFailed();
		business.markPastDue();

		subscriptionRepository.save(subscription);
		businessRepository.save(business);

		billingAuditService.record(
				business,
				BillingAuditAction.PAYMENT_FAILED,
				"paypal-webhook",
				paypalEventId,
				paypalSubscriptionId,
				"PayPal subscription payment failed. Business marked PAST_DUE"
		);
	}

	public void handlePayPalSubscriptionCancelled(String paypalSubscriptionId, String paypalEventId) {
		BusinessSubscription subscription = subscriptionByPayPalId(paypalSubscriptionId);
		Business business = subscription.getBusiness();

		subscription.cancel();
		business.deactivate();

		subscriptionRepository.save(subscription);
		businessRepository.save(business);

		billingAuditService.record(
				business,
				BillingAuditAction.SUBSCRIPTION_CANCELLED,
				"paypal-webhook",
				paypalEventId,
				paypalSubscriptionId,
				"PayPal subscription cancelled. Business deactivated"
		);
	}

	public void handlePayPalSubscriptionSuspended(String paypalSubscriptionId, String paypalEventId) {
		BusinessSubscription subscription = subscriptionByPayPalId(paypalSubscriptionId);
		Business business = subscription.getBusiness();

		subscription.markPaymentFailed();
		business.deactivate();

		subscriptionRepository.save(subscription);
		businessRepository.save(business);

		billingAuditService.record(
				business,
				BillingAuditAction.SUBSCRIPTION_SUSPENDED,
				"paypal-webhook",
				paypalEventId,
				paypalSubscriptionId,
				"PayPal subscription suspended. Business deactivated"
		);
	}

	public void handlePayPalSubscriptionExpired(String paypalSubscriptionId, String paypalEventId) {
		BusinessSubscription subscription = subscriptionByPayPalId(paypalSubscriptionId);
		Business business = subscription.getBusiness();

		subscription.expire();
		business.deactivate();

		subscriptionRepository.save(subscription);
		businessRepository.save(business);

		billingAuditService.record(
				business,
				BillingAuditAction.SUBSCRIPTION_EXPIRED,
				"paypal-webhook",
				paypalEventId,
				paypalSubscriptionId,
				"PayPal subscription expired. Business deactivated"
		);
	}

	public void deactivateExpiredTrialsAndUnpaidBusinesses() {
		LocalDateTime now = LocalDateTime.now(clock);

		for (Business business : businessRepository.findByBusinessStatusAndTrialEndDateBefore(BusinessStatus.TRIAL, now)) {
			business.deactivate();
			businessRepository.save(business);

			billingAuditService.record(
					business,
					BillingAuditAction.BUSINESS_DEACTIVATED,
					"billing-scheduler",
					null,
					business.getPaypalSubscriptionId(),
					"Free trial expired. Business deactivated"
			);
		}

		for (Business business : businessRepository.findByBusinessStatusAndCurrentBillingPeriodEndDateBefore(BusinessStatus.ACTIVE, now)) {
			business.deactivate();
			businessRepository.save(business);

			billingAuditService.record(
					business,
					BillingAuditAction.BUSINESS_DEACTIVATED,
					"billing-scheduler",
					null,
					business.getPaypalSubscriptionId(),
					"Billing period expired without confirmed payment. Business deactivated"
			);
		}
	}

	private void activateSubscription(
			BusinessSubscription subscription,
			String actorUsername,
			String paypalEventId,
			String message) {
		Business business = subscription.getBusiness();
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime periodEnd = periodEnd(now, subscription.getBillingInterval(), subscription.getTermYears());

		subscription.activate(now, periodEnd);

		business.activatePaidPlan(
				subscription.getBusinessPlan(),
				now,
				periodEnd,
				subscription.getPaypalSubscriptionId(),
				subscription.getPaypalSubscriptionToken(),
				subscription.getPaypalPlanId()
		);

		businessRepository.save(business);
		subscriptionRepository.save(subscription);

		billingAuditService.record(
				business,
				BillingAuditAction.PLAN_ACTIVATED,
				actorUsername,
				paypalEventId,
				subscription.getPaypalSubscriptionId(),
				message
		);
	}

	private BusinessSubscription subscriptionByPayPalId(String paypalSubscriptionId) {
		if (paypalSubscriptionId == null || paypalSubscriptionId.isBlank()) {
			throw new IllegalArgumentException("PayPal subscription id is required");
		}

		return subscriptionRepository.findByPaypalSubscriptionId(paypalSubscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for PayPal id: " + paypalSubscriptionId));
	}

	private BusinessSubscription latestSubscription(Business business) {
		return subscriptionRepository.findTopByBusiness_IdOrderByCreatedDateDesc(business.getId()).orElse(null);
	}

	private Business businessForOwner(String actorUsername) {
		TrackerUser owner = userRepository.findByUsername(actorUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));

		if (owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can manage billing");
		}

		if (owner.getBusiness() == null) {
			throw new IllegalArgumentException("Owner is not linked to a business");
		}

		return owner.getBusiness();
	}

	private Business businessForActor(String actorUsername) {
		TrackerUser user = userRepository.findByUsername(actorUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));

		if (user.getBusiness() == null) {
			throw new IllegalArgumentException("User is not linked to a business");
		}

		return user.getBusiness();
	}

	private void validateRequest(CreateBusinessSubscriptionRequest request) {
		if (request.plan() == null) {
			throw new IllegalArgumentException("Business plan is required");
		}

		if (request.billingInterval() == null) {
			throw new IllegalArgumentException("Billing interval is required");
		}

		if (request.billingInterval() == BillingInterval.YEARLY) {
			if (request.termYears() == null || request.termYears() < 1 || request.termYears() > 5) {
				throw new IllegalArgumentException("Yearly plan term must be between 1 and 5 years");
			}
		}
	}

	private Integer normalizedTermYears(CreateBusinessSubscriptionRequest request) {
		if (request.billingInterval() == BillingInterval.MONTHLY) {
			return null;
		}

		return request.termYears() == null ? 1 : request.termYears();
	}

	private BigDecimal amountFor(BusinessPlan plan, BillingInterval interval, Integer termYears) {
		BigDecimal monthlyAmount = businessPlanPolicyService.monthlyPrice(plan);

		if (interval == BillingInterval.MONTHLY) {
			return monthlyAmount;
		}

		int years = termYears == null ? 1 : termYears;
		return monthlyAmount.multiply(BigDecimal.valueOf(12L * years));
	}

	private LocalDateTime periodEnd(LocalDateTime start, BillingInterval interval, Integer termYears) {
		if (interval == BillingInterval.MONTHLY) {
			return start.plusMonths(1);
		}

		return start.plusYears(termYears == null ? 1 : termYears);
	}
}
