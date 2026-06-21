package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.AdminBusinessOverrideRequest;
import com.king_sparkon_tracker.backend.dto.BusinessBillingResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.AdminBusinessOverrideAction;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.BillingAuditAction;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.BusinessSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class AdminBusinessOverrideServiceTest {

	@Mock
	private BusinessRepository businessRepository;

	@Mock
	private BusinessSubscriptionRepository subscriptionRepository;

	@Mock
	private BillingAuditService billingAuditService;

	private AdminBusinessOverrideService service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneId.of("Africa/Johannesburg"));
		service = new AdminBusinessOverrideService(
				businessRepository,
				subscriptionRepository,
				billingAuditService,
				clock);
	}

	@Test
	void overrideBusinessReactivatesBusinessWithRequestedPlanAndDate() {
		Business business = business();
		LocalDateTime periodEnd = LocalDateTime.of(2026, 12, 31, 23, 59);
		when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
		when(businessRepository.save(business)).thenReturn(business);
		when(subscriptionRepository.findTopByBusiness_IdOrderByCreatedDateDesc(1L)).thenReturn(Optional.empty());

		BusinessBillingResponse response = service.overrideBusiness(
				1L,
				new AdminBusinessOverrideRequest(
						AdminBusinessOverrideAction.REACTIVATE,
						BusinessPlan.PRO,
						periodEnd,
						"manual payment confirmed"),
				"admin");

		assertThat(response.businessStatus()).isEqualTo(BusinessStatus.ACTIVE);
		assertThat(response.businessPlan()).isEqualTo(BusinessPlan.PRO);
		assertThat(business.getCurrentBillingPeriodEndDate()).isEqualTo(periodEnd);
		verify(billingAuditService).record(
				business,
				BillingAuditAction.ADMIN_OVERRIDE_ACTIVATED,
				"admin",
				null,
				null,
				"Admin reactivated business. Reason: manual payment confirmed");
	}

	@Test
	void overrideBusinessDeactivatesBusiness() {
		Business business = business();
		when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
		when(businessRepository.save(business)).thenReturn(business);
		when(subscriptionRepository.findTopByBusiness_IdOrderByCreatedDateDesc(1L)).thenReturn(Optional.empty());

		BusinessBillingResponse response = service.overrideBusiness(
				1L,
				new AdminBusinessOverrideRequest(
						AdminBusinessOverrideAction.DEACTIVATE,
						null,
						null,
						"trial abuse"),
				"admin");

		assertThat(response.businessStatus()).isEqualTo(BusinessStatus.DEACTIVATED);
		verify(billingAuditService).record(
				business,
				BillingAuditAction.ADMIN_OVERRIDE_DEACTIVATED,
				"admin",
				null,
				null,
				"Admin deactivated business. Reason: trial abuse");
	}

	@Test
	void overrideBusinessThrowsWhenBusinessIsMissing() {
		when(businessRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.overrideBusiness(
				404L,
				new AdminBusinessOverrideRequest(AdminBusinessOverrideAction.REACTIVATE, null, null, null),
				"admin"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Business not found: 404");
	}

	@Test
	void overrideBusinessMarksPastDue() {
		Business business = business();
		when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
		when(businessRepository.save(business)).thenReturn(business);
		when(subscriptionRepository.findTopByBusiness_IdOrderByCreatedDateDesc(1L)).thenReturn(Optional.empty());

		BusinessBillingResponse response = service.overrideBusiness(
				1L,
				new AdminBusinessOverrideRequest(AdminBusinessOverrideAction.MARK_PAST_DUE, null, null, null),
				"admin");

		assertThat(response.businessStatus()).isEqualTo(BusinessStatus.PAST_DUE);
		verify(billingAuditService).record(
				eq(business),
				eq(BillingAuditAction.PAYMENT_FAILED),
				eq("admin"),
				eq(null),
				eq(null),
				eq("Admin marked business past due"));
	}

	private Business business() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		business.setBusinessStatus(BusinessStatus.TRIAL);
		owner.setBusiness(business);
		return business;
	}
}
