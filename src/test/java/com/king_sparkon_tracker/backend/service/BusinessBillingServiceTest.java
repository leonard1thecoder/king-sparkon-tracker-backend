package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.dto.CreateBusinessSubscriptionRequest;
import com.king_sparkon_tracker.backend.dto.CreateStripeCheckoutSessionResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.outbox.OutboxEventType;
import com.king_sparkon_tracker.backend.outbox.OutboxPayloads;
import com.king_sparkon_tracker.backend.outbox.OutboxPublisher;
import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.SubscriptionPaymentStatus;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.BusinessSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class BusinessBillingServiceTest {

	@Mock
	private BusinessRepository businessRepository;

	@Mock
	private BusinessSubscriptionRepository subscriptionRepository;

	@Mock
	private TrackerUserRepository userRepository;

	@Mock
	private PayPalBillingClient payPalBillingClient;

	@Mock
	private StripeBillingClient stripeBillingClient;

	@Mock
	private BillingAuditService billingAuditService;

	@Mock
	private AppEmailService appEmailService;

	@Mock
	private OutboxPublisher outboxPublisher;

	@Mock
	private TrackerUserService trackerUserService;

	private BusinessBillingService service;
	private Clock fixedClock;

	@BeforeEach
	void setUp() {
		fixedClock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("Africa/Johannesburg"));
		service = new BusinessBillingService(
				businessRepository,
				subscriptionRepository,
				userRepository,
				new BusinessPlanPolicyService(),
				payPalBillingClient,
				stripeBillingClient,
				billingAuditService,
				fixedClock,
				appEmailService,
				outboxPublisher,
				trackerUserService);
	}

	@Test
	void createSubscriptionCreatesPayPalApprovalForPaidPlan() {
		TrackerUser owner = owner();
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
		when(subscriptionRepository.save(any(BusinessSubscription.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(payPalBillingClient.createSubscription(BusinessPlan.PLUS, BillingInterval.YEARLY, 3))
				.thenReturn(new PayPalBillingClient.CreatedPayPalSubscription(
						"I-SUB-123",
						"I-SUB-123",
						"P-PLUS-YEAR",
						"APPROVAL_PENDING",
						"https://paypal.example/approve"));

		BusinessBillingResponse response = service.createSubscription(
				"owner",
				new CreateBusinessSubscriptionRequest(BusinessPlan.PLUS, BillingInterval.YEARLY, 3));

		assertThat(response.businessId()).isEqualTo(1L);
		assertThat(response.businessPlan()).isEqualTo(BusinessPlan.FREE_TRIAL);
		assertThat(response.billingInterval()).isEqualTo(BillingInterval.YEARLY);
		assertThat(response.termYears()).isEqualTo(3);
		assertThat(response.amount()).isEqualByComparingTo("31680.00");
		assertThat(response.paymentStatus()).isEqualTo(SubscriptionPaymentStatus.APPROVAL_PENDING);
		assertThat(response.paypalApprovalUrl()).isEqualTo("https://paypal.example/approve");
		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PLAN_APPROVAL_PENDING,
				"owner",
				null,
				"I-SUB-123",
				"PayPal approval pending for PLUS");
	}

	@Test
	void createSubscriptionRejectsFreeTrialPurchase() {
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner()));

		assertThatThrownBy(() -> service.createSubscription(
				"owner",
				new CreateBusinessSubscriptionRequest(BusinessPlan.FREE_TRIAL, BillingInterval.MONTHLY, null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("FREE_TRIAL cannot be purchased");
	}

	@Test
	void createStripeCheckoutSessionCreatesHostedCheckoutForPaidPlan() {
		TrackerUser owner = owner();
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
		when(subscriptionRepository.save(any(BusinessSubscription.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(stripeBillingClient.createCheckoutSession(eq(owner.getBusiness()), any(BusinessSubscription.class)))
				.thenReturn(new StripeBillingClient.CreatedStripeCheckoutSession(
						"cs_live_123",
						"https://checkout.stripe.com/c/pay/cs_live_123",
						"PLUS-MONTHLY-1"));

		CreateStripeCheckoutSessionResponse response = service.createStripeCheckoutSession(
				"owner",
				new CreateBusinessSubscriptionRequest(BusinessPlan.PLUS, BillingInterval.MONTHLY, null));

		assertThat(response.checkoutSessionId()).isEqualTo("cs_live_123");
		assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_live_123");
		assertThat(response.paymentStatus()).isEqualTo(SubscriptionPaymentStatus.APPROVAL_PENDING);
		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PLAN_APPROVAL_PENDING,
				"owner",
				null,
				"cs_live_123",
				"Stripe checkout pending for PLUS");
	}

	@Test
	void activateApprovedSubscriptionActivatesBusinessAndSubscription() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PRO, BillingInterval.MONTHLY, null);
		ReflectionTestUtils.setField(subscription, "id", 99L);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PRO-MONTH", "https://paypal.example/approve");

		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
		when(subscriptionRepository.findById(99L)).thenReturn(Optional.of(subscription));
		when(payPalBillingClient.subscriptionStatus("I-SUB-123")).thenReturn("ACTIVE");
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		BusinessBillingResponse response = service.activateApprovedSubscription("owner", 99L);

		assertThat(response.businessPlan()).isEqualTo(BusinessPlan.PRO);
		assertThat(response.businessStatus()).isEqualTo(BusinessStatus.ACTIVE);
		assertThat(owner.getBusiness().getPaypalSubscriptionId()).isEqualTo("I-SUB-123");
		assertThat(owner.getBusiness().getCurrentBillingPeriodEndDate())
				.isEqualTo(java.time.LocalDateTime.of(2026, 7, 1, 12, 0));
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionPaymentStatus.ACTIVE);
		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PLAN_ACTIVATED,
				"owner",
				null,
				"I-SUB-123",
				"Manual approval activation");
		verify(appEmailService).sendBillingActivatedEmail(
				owner.getBusiness(),
				subscription,
				"Manual approval activation");
		verify(outboxPublisher).publish(
				eq("BUSINESS_SUBSCRIPTION"),
				eq("99"),
				eq(OutboxEventType.AFFILIATE_COMMISSION_CALCULATION),
				any(OutboxPayloads.AffiliateCalculation.class),
				eq("affiliate-commission:99"));
	}

	@Test
	void activateApprovedSubscriptionContinuesWhenActivatedEmailFails() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PRO, BillingInterval.MONTHLY, null);
		ReflectionTestUtils.setField(subscription, "id", 99L);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PRO-MONTH", "https://paypal.example/approve");
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
		when(subscriptionRepository.findById(99L)).thenReturn(Optional.of(subscription));
		when(payPalBillingClient.subscriptionStatus("I-SUB-123")).thenReturn("ACTIVE");
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		doThrow(new IllegalStateException("smtp down"))
				.when(appEmailService)
				.sendBillingActivatedEmail(owner.getBusiness(), subscription, "Manual approval activation");

		BusinessBillingResponse response = service.activateApprovedSubscription("owner", 99L);

		assertThat(response.businessStatus()).isEqualTo(BusinessStatus.ACTIVE);
	}

	@Test
	void activateApprovedSubscriptionRejectsForeignSubscription() {
		TrackerUser owner = owner();
		Business otherBusiness = business("Other Store", 2L);
		BusinessSubscription subscription = subscription(otherBusiness, BusinessPlan.PRO, BillingInterval.MONTHLY, null);
		ReflectionTestUtils.setField(subscription, "id", 99L);
		when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
		when(subscriptionRepository.findById(99L)).thenReturn(Optional.of(subscription));

		assertThatThrownBy(() -> service.activateApprovedSubscription("owner", 99L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Subscription does not belong to this business");
	}

	@Test
	void currentBillingRejectsWorkers() {
		TrackerUser worker = new TrackerUser(
				"worker",
				"worker@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Worker));
		worker.setBusiness(business("Worker Store", 1L));
		when(userRepository.findByUsername("worker")).thenReturn(Optional.of(worker));

		assertThatThrownBy(() -> service.currentBilling("worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Only business owners can manage billing");
	}

	@Test
	void handlePayPalPaymentFailedMarksBusinessPastDue() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PLUS-MONTH", null);
		when(subscriptionRepository.findByPaypalSubscriptionId("I-SUB-123")).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		service.handlePayPalPaymentFailed("I-SUB-123", "EVT-FAILED");

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionPaymentStatus.PAYMENT_FAILED);
		assertThat(owner.getBusiness().getBusinessStatus()).isEqualTo(BusinessStatus.PAST_DUE);
		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PAYMENT_FAILED,
				"paypal-webhook",
				"EVT-FAILED",
				"I-SUB-123",
				"PayPal subscription payment failed. Business marked PAST_DUE");
		verify(appEmailService).sendBillingPaymentFailedEmail(
				owner.getBusiness(),
				subscription,
				"PayPal subscription payment failed. Business marked PAST_DUE");
	}

	@Test
	void handlePayPalSubscriptionActivatedSendsActivatedEmail() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PLUS-MONTH", null);
		when(subscriptionRepository.findByPaypalSubscriptionId("I-SUB-123")).thenReturn(Optional.of(subscription));
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		service.handlePayPalSubscriptionActivated("I-SUB-123", "EVT-ACTIVE");

		verify(appEmailService).sendBillingActivatedEmail(
				owner.getBusiness(),
				subscription,
				"PayPal subscription activated");
	}

	@Test
	void handleStripeCheckoutSessionCompletedActivatesBusinessAndSubscription() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		ReflectionTestUtils.setField(subscription, "id", 77L);
		subscription.markStripeCheckoutPending("cs_live_123", "PLUS-MONTHLY-1", "https://checkout.stripe.com/c/pay/cs_live_123");
		when(subscriptionRepository.findByStripeCheckoutSessionId("cs_live_123")).thenReturn(Optional.of(subscription));
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		service.handleStripeCheckoutSessionCompleted("cs_live_123", "sub_live_123", "evt_123");

		assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_live_123");
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionPaymentStatus.ACTIVE);
		assertThat(owner.getBusiness().getStripeSubscriptionId()).isEqualTo("sub_live_123");
		assertThat(owner.getBusiness().getCurrentBillingPeriodEndDate())
				.isEqualTo(java.time.LocalDateTime.of(2026, 7, 1, 12, 0));
		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PLAN_ACTIVATED,
				"stripe-webhook",
				"evt_123",
				"sub_live_123",
				"Stripe checkout completed");
		verify(appEmailService).sendBillingActivatedEmail(
				owner.getBusiness(),
				subscription,
				"Stripe checkout completed");
	}

	@Test
	void handleStripeInvoicePaymentSucceededOnlyAuditsWhenAlreadyCurrent() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markStripeCheckoutPending("cs_live_123", "PLUS-MONTHLY-1", "https://checkout.stripe.com/c/pay/cs_live_123");
		subscription.markStripeSubscription("sub_live_123");
		subscription.activate(
				java.time.LocalDateTime.of(2026, 6, 1, 12, 0),
				java.time.LocalDateTime.of(2026, 7, 1, 12, 0));
		owner.getBusiness().activateStripePaidPlan(
				BusinessPlan.PLUS,
				java.time.LocalDateTime.of(2026, 6, 1, 12, 0),
				java.time.LocalDateTime.of(2026, 7, 1, 12, 0),
				"sub_live_123",
				"PLUS-MONTHLY-1");
		when(subscriptionRepository.findByStripeSubscriptionId("sub_live_123")).thenReturn(Optional.of(subscription));

		service.handleStripeInvoicePaymentSucceeded("sub_live_123", "evt_paid");

		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PAYMENT_SUCCESS,
				"stripe-webhook",
				"evt_paid",
				"sub_live_123",
				"Stripe subscription payment confirmed");
	}

	@Test
	void handlePayPalPaymentCompletedSendsActivatedEmail() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PLUS-MONTH", null);
		when(subscriptionRepository.findByPaypalSubscriptionId("I-SUB-123")).thenReturn(Optional.of(subscription));
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		service.handlePayPalPaymentCompleted("I-SUB-123", "EVT-PAID");

		verify(appEmailService).sendBillingActivatedEmail(
				owner.getBusiness(),
				subscription,
				"PayPal subscription payment completed");
	}

	@Test
	void handlePayPalSubscriptionCancelledSendsCancelledEmail() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PLUS-MONTH", null);
		when(subscriptionRepository.findByPaypalSubscriptionId("I-SUB-123")).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		service.handlePayPalSubscriptionCancelled("I-SUB-123", "EVT-CANCEL");

		verify(appEmailService).sendBillingCancelledEmail(
				owner.getBusiness(),
				subscription,
				"PayPal subscription cancelled. Business deactivated");
	}

	@Test
	void handleStripeInvoicePaymentFailedMarksBusinessPastDue() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markStripeCheckoutPending("cs_live_123", "PLUS-MONTHLY-1", "https://checkout.stripe.com/c/pay/cs_live_123");
		subscription.markStripeSubscription("sub_live_123");
		when(subscriptionRepository.findByStripeSubscriptionId("sub_live_123")).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		service.handleStripeInvoicePaymentFailed("sub_live_123", "evt_failed");

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionPaymentStatus.PAYMENT_FAILED);
		assertThat(owner.getBusiness().getBusinessStatus()).isEqualTo(BusinessStatus.PAST_DUE);
		verify(billingAuditService).record(
				owner.getBusiness(),
				BillingAuditAction.PAYMENT_FAILED,
				"stripe-webhook",
				"evt_failed",
				"sub_live_123",
				"Stripe subscription payment failed. Business marked PAST_DUE");
		verify(appEmailService).sendBillingPaymentFailedEmail(
				owner.getBusiness(),
				subscription,
				"Stripe subscription payment failed. Business marked PAST_DUE");
	}

	@Test
	void handleStripeSubscriptionCancelledDeactivatesBusiness() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markStripeCheckoutPending("cs_live_123", "PLUS-MONTHLY-1", "https://checkout.stripe.com/c/pay/cs_live_123");
		subscription.markStripeSubscription("sub_live_123");
		when(subscriptionRepository.findByStripeSubscriptionId("sub_live_123")).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		service.handleStripeSubscriptionCancelled("sub_live_123", "evt_deleted");

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionPaymentStatus.CANCELLED);
		assertThat(owner.getBusiness().getBusinessStatus()).isEqualTo(BusinessStatus.DEACTIVATED);
		verify(appEmailService).sendBillingCancelledEmail(
				owner.getBusiness(),
				subscription,
				"Stripe subscription cancelled. Business deactivated");
	}

	@Test
	void handlePayPalSubscriptionSuspendedSendsSuspendedEmail() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PLUS-MONTH", null);
		when(subscriptionRepository.findByPaypalSubscriptionId("I-SUB-123")).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		service.handlePayPalSubscriptionSuspended("I-SUB-123", "EVT-SUSPEND");

		verify(appEmailService).sendBillingSuspendedEmail(
				owner.getBusiness(),
				subscription,
				"PayPal subscription suspended. Business deactivated");
	}

	@Test
	void handlePayPalSubscriptionExpiredSendsExpiredEmail() {
		TrackerUser owner = owner();
		BusinessSubscription subscription = subscription(owner.getBusiness(), BusinessPlan.PLUS, BillingInterval.MONTHLY, null);
		subscription.markApprovalPending("I-SUB-123", "I-SUB-123", "P-PLUS-MONTH", null);
		when(subscriptionRepository.findByPaypalSubscriptionId("I-SUB-123")).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		service.handlePayPalSubscriptionExpired("I-SUB-123", "EVT-EXPIRED");

		verify(appEmailService).sendBillingExpiredEmail(
				owner.getBusiness(),
				subscription,
				"PayPal subscription expired. Business deactivated");
	}

	@Test
	void deactivateExpiredTrialsAndUnpaidBusinessesDeactivatesBothGroups() {
		Business expiredTrial = business("Trial Store", 1L);
		expiredTrial.setBusinessStatus(BusinessStatus.TRIAL);
		Business expiredActive = business("Paid Store", 2L);
		expiredActive.setBusinessStatus(BusinessStatus.ACTIVE);

		when(businessRepository.findByBusinessStatusAndTrialEndDateBefore(
				eq(BusinessStatus.TRIAL),
				any(java.time.LocalDateTime.class))).thenReturn(List.of(expiredTrial));
		when(businessRepository.findByBusinessStatusAndCurrentBillingPeriodEndDateBefore(
				eq(BusinessStatus.ACTIVE),
				any(java.time.LocalDateTime.class))).thenReturn(List.of(expiredActive));
		when(businessRepository.save(any(Business.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.deactivateExpiredTrialsAndUnpaidBusinesses();

		assertThat(expiredTrial.getBusinessStatus()).isEqualTo(BusinessStatus.DEACTIVATED);
		assertThat(expiredActive.getBusinessStatus()).isEqualTo(BusinessStatus.DEACTIVATED);
		verify(billingAuditService).record(
				eq(expiredTrial),
				eq(BillingAuditAction.BUSINESS_DEACTIVATED),
				eq("billing-scheduler"),
				eq(null),
				eq(null),
				eq("Free trial expired. Business deactivated"));
		verify(billingAuditService).record(
				eq(expiredActive),
				eq(BillingAuditAction.BUSINESS_DEACTIVATED),
				eq("billing-scheduler"),
				eq(null),
				eq(null),
				eq("Billing period expired without confirmed payment. Business deactivated"));
		verify(appEmailService).sendBusinessDeactivatedEmail(expiredTrial, "Free trial expired. Business deactivated");
		verify(appEmailService).sendBusinessDeactivatedEmail(expiredActive, "Billing period expired without confirmed payment. Business deactivated");
	}

	@Test
	void deactivateExpiredTrialsContinuesWhenDeactivatedEmailFails() {
		Business expiredTrial = business("Trial Store", 1L);
		expiredTrial.setBusinessStatus(BusinessStatus.TRIAL);
		when(businessRepository.findByBusinessStatusAndTrialEndDateBefore(
				eq(BusinessStatus.TRIAL),
				any(java.time.LocalDateTime.class))).thenReturn(List.of(expiredTrial));
		when(businessRepository.findByBusinessStatusAndCurrentBillingPeriodEndDateBefore(
				eq(BusinessStatus.ACTIVE),
				any(java.time.LocalDateTime.class))).thenReturn(List.of());
		when(businessRepository.save(expiredTrial)).thenReturn(expiredTrial);
		doThrow(new IllegalStateException("smtp down"))
				.when(appEmailService)
				.sendBusinessDeactivatedEmail(expiredTrial, "Free trial expired. Business deactivated");

		service.deactivateExpiredTrialsAndUnpaidBusinesses();

		assertThat(expiredTrial.getBusinessStatus()).isEqualTo(BusinessStatus.DEACTIVATED);
	}

	@Test
	void handlePayPalSubscriptionRequiresSubscriptionId() {
		assertThatThrownBy(() -> service.handlePayPalSubscriptionActivated(" ", "EVT-1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("PayPal subscription id is required");
	}

	@Test
	void handleStripeCheckoutRequiresSubscriptionId() {
		assertThatThrownBy(() -> service.handleStripeCheckoutSessionCompleted("cs_live_123", " ", "evt_123"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Stripe subscription id is required");
	}

	@Test
	void handlePayPalSubscriptionThrowsWhenUnknown() {
		when(subscriptionRepository.findByPaypalSubscriptionId("I-MISSING")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.handlePayPalSubscriptionActivated("I-MISSING", "EVT-1"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Subscription not found for PayPal id: I-MISSING");
	}

	@Test
	void handleStripeSubscriptionThrowsWhenUnknown() {
		when(subscriptionRepository.findByStripeSubscriptionId("sub_missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.handleStripeInvoicePaymentSucceeded("sub_missing", "evt_1"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Subscription not found for Stripe id: sub_missing");
	}

	private TrackerUser owner() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		return owner;
	}

	private Business business(String name, Long id) {
		TrackerUser owner = new TrackerUser(
				name.toLowerCase().replace(" ", "-"),
				name.toLowerCase().replace(" ", "-") + "@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business(name, owner);
		ReflectionTestUtils.setField(business, "id", id);
		owner.setBusiness(business);
		return business;
	}

	private BusinessSubscription subscription(
			Business business,
			BusinessPlan plan,
			BillingInterval interval,
			Integer termYears) {
		return new BusinessSubscription(
				business,
				plan,
				interval,
				termYears,
				new java.math.BigDecimal("2300.00"),
				"ZAR");
	}
}
