package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.BillingDashboardResponse;
import com.king_sparkon_tracker.backend.dto.BillingPlanResponse;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.dto.CreateBusinessSubscriptionRequest;
import com.king_sparkon_tracker.backend.dto.CreateStripeCheckoutSessionResponse;
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
import com.king_sparkon_tracker.backend.model.SubscriptionPaymentStatus;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.BusinessSubscriptionRepository;
import com.king_sparkon_tracker.backend.outbox.OutboxEventType;
import com.king_sparkon_tracker.backend.outbox.OutboxPayloads;
import com.king_sparkon_tracker.backend.outbox.OutboxPublisher;

@Service
@Transactional
public class BusinessBillingService {

	private static final Logger log = LoggerFactory.getLogger(BusinessBillingService.class);

	private final BusinessRepository businessRepository;
	private final BusinessSubscriptionRepository subscriptionRepository;
	private final TrackerUserRepository userRepository;
	private final BusinessPlanPolicyService businessPlanPolicyService;
	private final PayPalBillingClient payPalBillingClient;
	private final StripeBillingClient stripeBillingClient;
	private final BillingAuditService billingAuditService;
	private final Clock clock;
	private final AppEmailService appEmailService;
	private final OutboxPublisher outboxPublisher;
	private final TrackerUserService trackerUserService;

	public BusinessBillingService(
			BusinessRepository businessRepository,
			BusinessSubscriptionRepository subscriptionRepository,
			TrackerUserRepository userRepository,
			BusinessPlanPolicyService businessPlanPolicyService,
			PayPalBillingClient payPalBillingClient,
			StripeBillingClient stripeBillingClient,
			BillingAuditService billingAuditService,
			Clock clock,
			AppEmailService appEmailService,
			OutboxPublisher outboxPublisher,
			TrackerUserService trackerUserService) {
		this.businessRepository = businessRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.userRepository = userRepository;
		this.businessPlanPolicyService = businessPlanPolicyService;
		this.payPalBillingClient = payPalBillingClient;
		this.stripeBillingClient = stripeBillingClient;
		this.billingAuditService = billingAuditService;
		this.clock = clock;
		this.appEmailService = appEmailService;
		this.outboxPublisher = outboxPublisher;
		this.trackerUserService = trackerUserService;
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
		validatePaidRequest(request);
		trackerUserService.applyAffiliateReferral(business, request.affiliateCode());

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

	public CreateStripeCheckoutSessionResponse createStripeCheckoutSession(
			String actorUsername,
			CreateBusinessSubscriptionRequest request) {
		Business business = businessForOwner(actorUsername);
		validatePaidRequest(request);
		trackerUserService.applyAffiliateReferral(business, request.affiliateCode());

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

		StripeBillingClient.CreatedStripeCheckoutSession checkoutSession =
				stripeBillingClient.createCheckoutSession(business, subscription);

		subscription.markStripeCheckoutPending(
				checkoutSession.checkoutSessionId(),
				checkoutSession.priceReference(),
				checkoutSession.checkoutUrl()
		);

		BusinessSubscription savedSubscription = subscriptionRepository.save(subscription);

		billingAuditService.record(
				business,
				BillingAuditAction.PLAN_APPROVAL_PENDING,
				actorUsername,
				null,
				savedSubscription.getStripeCheckoutSessionId(),
				"Stripe checkout pending for " + savedSubscription.getBusinessPlan()
		);

		return new CreateStripeCheckoutSessionResponse(
				savedSubscription.getId(),
				savedSubscription.getStripeCheckoutSessionId(),
				savedSubscription.getStripeCheckoutUrl(),
				savedSubscription.getStatus()
		);
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

		sendBillingPaymentFailedNotification(business, subscription, "PayPal subscription payment failed. Business marked PAST_DUE");
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

		sendBillingCancelledNotification(business, subscription, "PayPal subscription cancelled. Business deactivated");
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

		sendBillingSuspendedNotification(business, subscription, "PayPal subscription suspended. Business deactivated");
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

		sendBillingExpiredNotification(business, subscription, "PayPal subscription expired. Business deactivated");
	}

	public void handleStripeCheckoutSessionCompleted(
			String stripeCheckoutSessionId,
			String stripeSubscriptionId,
			String stripeEventId) {
		if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
			throw new IllegalArgumentException("Stripe subscription id is required");
		}

		BusinessSubscription subscription = subscriptionByStripeCheckoutSessionId(stripeCheckoutSessionId);
		subscription.markStripeSubscription(stripeSubscriptionId);

		if (subscription.getStatus() == SubscriptionPaymentStatus.ACTIVE) {
			subscriptionRepository.save(subscription);
			billingAuditService.record(
					subscription.getBusiness(),
					BillingAuditAction.PLAN_ACTIVATED,
					"stripe-webhook",
					stripeEventId,
					stripeSubscriptionId,
					"Stripe checkout completed for already active subscription"
			);
			return;
		}

		activateStripeSubscription(subscription, "stripe-webhook", stripeEventId, "Stripe checkout completed");
	}

	public void handleStripeInvoicePaymentSucceeded(String stripeSubscriptionId, String stripeEventId) {
		BusinessSubscription subscription = subscriptionByStripeSubscriptionId(stripeSubscriptionId);
		Business business = subscription.getBusiness();
		LocalDateTime now = LocalDateTime.now(clock);

		if (subscription.getStatus() == SubscriptionPaymentStatus.ACTIVE
				&& business.getCurrentBillingPeriodEndDate() != null
				&& business.getCurrentBillingPeriodEndDate().isAfter(now)) {
			billingAuditService.record(
					business,
					BillingAuditAction.PAYMENT_SUCCESS,
					"stripe-webhook",
					stripeEventId,
					stripeSubscriptionId,
					"Stripe subscription payment confirmed"
			);
			return;
		}

		activateStripeSubscription(subscription, "stripe-webhook", stripeEventId, "Stripe subscription payment completed");

		billingAuditService.record(
				business,
				BillingAuditAction.PAYMENT_SUCCESS,
				"stripe-webhook",
				stripeEventId,
				stripeSubscriptionId,
				"Stripe subscription payment completed"
		);
	}

	public void handleStripeInvoicePaymentFailed(String stripeSubscriptionId, String stripeEventId) {
		BusinessSubscription subscription = subscriptionByStripeSubscriptionId(stripeSubscriptionId);
		Business business = subscription.getBusiness();

		subscription.markPaymentFailed();
		business.markPastDue();

		subscriptionRepository.save(subscription);
		businessRepository.save(business);

		billingAuditService.record(
				business,
				BillingAuditAction.PAYMENT_FAILED,
				"stripe-webhook",
				stripeEventId,
				stripeSubscriptionId,
				"Stripe subscription payment failed. Business marked PAST_DUE"
		);

		sendBillingPaymentFailedNotification(business, subscription, "Stripe subscription payment failed. Business marked PAST_DUE");
	}

	public void handleStripeSubscriptionCancelled(String stripeSubscriptionId, String stripeEventId) {
		BusinessSubscription subscription = subscriptionByStripeSubscriptionId(stripeSubscriptionId);
		Business business = subscription.getBusiness();

		subscription.cancel();
		business.deactivate();

		subscriptionRepository.save(subscription);
		businessRepository.save(business);

		billingAuditService.record(
				business,
				BillingAuditAction.SUBSCRIPTION_CANCELLED,
				"stripe-webhook",
				stripeEventId,
				stripeSubscriptionId,
				"Stripe subscription cancelled. Business deactivated"
		);

		sendBillingCancelledNotification(business, subscription, "Stripe subscription cancelled. Business deactivated");
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

			sendBusinessDeactivatedNotification(business, "Free trial expired. Business deactivated");
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

			sendBusinessDeactivatedNotification(business, "Billing period expired without confirmed payment. Business deactivated");
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

		queueAffiliateCommission(subscription, business, now);
		sendBillingActivatedNotification(business, subscription, message);
	}

	private void activateStripeSubscription(
			BusinessSubscription subscription,
			String actorUsername,
			String stripeEventId,
			String message) {
		Business business = subscription.getBusiness();
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime periodEnd = periodEnd(now, subscription.getBillingInterval(), subscription.getTermYears());

		subscription.activate(now, periodEnd);

		business.activateStripePaidPlan(
				subscription.getBusinessPlan(),
				now,
				periodEnd,
				subscription.getStripeSubscriptionId(),
				subscription.getStripePriceId()
		);

		businessRepository.save(business);
		subscriptionRepository.save(subscription);

		billingAuditService.record(
				business,
				BillingAuditAction.PLAN_ACTIVATED,
				actorUsername,
				stripeEventId,
				subscription.getStripeSubscriptionId(),
				message
		);

		queueAffiliateCommission(subscription, business, now);
		sendBillingActivatedNotification(business, subscription, message);
	}

	private void queueAffiliateCommission(BusinessSubscription subscription, Business business, LocalDateTime earnedAt) {
		if (subscription == null || subscription.getId() == null || business == null || business.getId() == null) {
			return;
		}
		outboxPublisher.publish(
				"BUSINESS_SUBSCRIPTION",
				String.valueOf(subscription.getId()),
				OutboxEventType.AFFILIATE_COMMISSION_CALCULATION,
				new OutboxPayloads.AffiliateCalculation(subscription.getId(), business.getId(), earnedAt),
				"affiliate-commission:" + subscription.getId());
	}

	private BusinessSubscription subscriptionByPayPalId(String paypalSubscriptionId) {
		if (paypalSubscriptionId == null || paypalSubscriptionId.isBlank()) {
			throw new IllegalArgumentException("PayPal subscription id is required");
		}

		return subscriptionRepository.findByPaypalSubscriptionId(paypalSubscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for PayPal id: " + paypalSubscriptionId));
	}

	private BusinessSubscription subscriptionByStripeCheckoutSessionId(String stripeCheckoutSessionId) {
		if (stripeCheckoutSessionId == null || stripeCheckoutSessionId.isBlank()) {
			throw new IllegalArgumentException("Stripe checkout session id is required");
		}

		return subscriptionRepository.findByStripeCheckoutSessionId(stripeCheckoutSessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for Stripe checkout session id: " + stripeCheckoutSessionId));
	}

	private BusinessSubscription subscriptionByStripeSubscriptionId(String stripeSubscriptionId) {
		if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
			throw new IllegalArgumentException("Stripe subscription id is required");
		}

		return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for Stripe id: " + stripeSubscriptionId));
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

	private void validatePaidRequest(CreateBusinessSubscriptionRequest request) {
		if (request.plan() == BusinessPlan.FREE_TRIAL) {
			throw new IllegalArgumentException("FREE_TRIAL cannot be purchased");
		}

		validateRequest(request);
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

	private void sendBillingActivatedNotification(Business business, BusinessSubscription subscription, String message) {
		try {
			appEmailService.sendBillingActivatedEmail(business, subscription, message);
		} catch (RuntimeException exception) {
			log.warn(
					"billing_activated_email_failed_non_blocking recipient={} businessId={} subscriptionId={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					subscription.getId(),
					exception.getMessage());
		}
	}

	private void sendBillingPaymentFailedNotification(Business business, BusinessSubscription subscription, String message) {
		try {
			appEmailService.sendBillingPaymentFailedEmail(business, subscription, message);
		} catch (RuntimeException exception) {
			log.warn(
					"billing_payment_failed_email_failed_non_blocking recipient={} businessId={} subscriptionId={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					subscription.getId(),
					exception.getMessage());
		}
	}

	private void sendBillingCancelledNotification(Business business, BusinessSubscription subscription, String message) {
		try {
			appEmailService.sendBillingCancelledEmail(business, subscription, message);
		} catch (RuntimeException exception) {
			log.warn(
					"billing_cancelled_email_failed_non_blocking recipient={} businessId={} subscriptionId={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					subscription.getId(),
					exception.getMessage());
		}
	}

	private void sendBillingSuspendedNotification(Business business, BusinessSubscription subscription, String message) {
		try {
			appEmailService.sendBillingSuspendedEmail(business, subscription, message);
		} catch (RuntimeException exception) {
			log.warn(
					"billing_suspended_email_failed_non_blocking recipient={} businessId={} subscriptionId={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					subscription.getId(),
					exception.getMessage());
		}
	}

	private void sendBillingExpiredNotification(Business business, BusinessSubscription subscription, String message) {
		try {
			appEmailService.sendBillingExpiredEmail(business, subscription, message);
		} catch (RuntimeException exception) {
			log.warn(
					"billing_expired_email_failed_non_blocking recipient={} businessId={} subscriptionId={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					subscription.getId(),
					exception.getMessage());
		}
	}

	private void sendBusinessDeactivatedNotification(Business business, String message) {
		try {
			appEmailService.sendBusinessDeactivatedEmail(business, message);
		} catch (RuntimeException exception) {
			log.warn(
					"business_deactivated_email_failed_non_blocking recipient={} businessId={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					exception.getMessage());
		}
	}
}
