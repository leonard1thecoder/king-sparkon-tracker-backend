package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.AffiliateOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.AffiliateProfileResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.AffiliateWithdrawalResponse;
import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.model.AffiliateCommission;
import com.king_sparkon_tracker.backend.model.AffiliateCommissionStatus;
import com.king_sparkon_tracker.backend.model.AffiliateWithdrawal;
import com.king_sparkon_tracker.backend.model.AffiliateWithdrawalStatus;
import com.king_sparkon_tracker.backend.model.BillingInterval;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.AffiliateCommissionRepository;
import com.king_sparkon_tracker.backend.repository.AffiliateWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.repository.TipWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class AffiliateServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	@Mock
	private AffiliateCommissionRepository commissionRepository;

	@Mock
	private AffiliateWithdrawalRepository withdrawalRepository;

	@Mock
	private TipRepository tipRepository;

	@Mock
	private TipWithdrawalRepository tipWithdrawalRepository;

	@Mock
	private PriceLocalizationService priceLocalizationService;

	@Mock
	private NotificationService notificationService;

	private AffiliateService service;

	@BeforeEach
	void setUp() {
		service = new AffiliateService(
				userRepository,
				commissionRepository,
				withdrawalRepository,
				tipRepository,
				tipWithdrawalRepository,
				priceLocalizationService,
				notificationService,
				Clock.fixed(Instant.parse("2026-06-24T10:00:00Z"), ZoneId.of("Africa/Johannesburg")),
				new BigDecimal("1000.00"),
				7,
				new BigDecimal("8.50"));
	}

	@Test
	void completeOnboardingStoresAffiliatePayoutContactFields() {
		TrackerUser affiliate = affiliate();
		when(userRepository.findByUsername("affiliate")).thenReturn(Optional.of(affiliate));
		when(userRepository.save(affiliate)).thenReturn(affiliate);

		AffiliateProfileResponse response = service.completeOnboarding(
				new AffiliateOnboardingRequest(" 12 Main Road ", " +27825550123 ", " https://paypal.me/affiliate "),
				"affiliate");

		assertThat(response.physicalAddress()).isEqualTo("12 Main Road");
		assertThat(response.cellphoneNumber()).isEqualTo("+27825550123");
		assertThat(response.paypalLink()).isEqualTo("https://paypal.me/affiliate");
		assertThat(response.onboardingCompleted()).isTrue();
	}

	@Test
	void commissionRateUsesAgeBasedAffiliateSchedule() {
		TrackerUser affiliate = affiliate();
		ReflectionTestUtils.setField(affiliate, "affiliateJoinedAt", LocalDateTime.of(2026, 1, 1, 10, 0));

		assertThat(service.commissionRatePercent(affiliate, LocalDateTime.of(2026, 3, 31, 10, 0)))
				.isEqualByComparingTo("18.00");
		assertThat(service.commissionRatePercent(affiliate, LocalDateTime.of(2026, 4, 1, 10, 0)))
				.isEqualByComparingTo("23.00");
		assertThat(service.commissionRatePercent(affiliate, LocalDateTime.of(2027, 1, 1, 10, 0)))
				.isEqualByComparingTo("28.00");
	}

	@Test
	void recordCommissionCreatesOneCommissionForSubscriptionRevenue() {
		TrackerUser affiliate = affiliate();
		ReflectionTestUtils.setField(affiliate, "affiliateJoinedAt", LocalDateTime.of(2026, 1, 1, 10, 0));
		Business business = business(affiliate);
		BusinessSubscription subscription = subscription(business, "1000.00");
		when(commissionRepository.existsBySubscription_Id(44L)).thenReturn(false);
		when(commissionRepository.save(any(AffiliateCommission.class))).thenAnswer(invocation -> {
			AffiliateCommission commission = invocation.getArgument(0);
			ReflectionTestUtils.setField(commission, "id", 77L);
			return commission;
		});

		AffiliateCommission commission = service.recordCommission(
				subscription,
				business,
				LocalDateTime.of(2026, 4, 1, 10, 0));

		assertThat(commission.getAffiliate()).isSameAs(affiliate);
		assertThat(commission.getBusiness()).isSameAs(business);
		assertThat(commission.getSubscription()).isSameAs(subscription);
		assertThat(commission.getGrossAmount()).isEqualByComparingTo("1000.00");
		assertThat(commission.getCommissionRatePercent()).isEqualByComparingTo("23.00");
		assertThat(commission.getCommissionAmount()).isEqualByComparingTo("230.00");
		assertThat(commission.getStatus()).isEqualTo(AffiliateCommissionStatus.EARNED);
		verify(notificationService).logAffiliateCommissionEarned(commission);
	}

	@Test
	void recordCommissionIsIdempotentBySubscription() {
		TrackerUser affiliate = affiliate();
		Business business = business(affiliate);
		BusinessSubscription subscription = subscription(business, "1000.00");
		when(commissionRepository.existsBySubscription_Id(44L)).thenReturn(true);

		assertThat(service.recordCommission(subscription, business, LocalDateTime.of(2026, 4, 1, 10, 0))).isNull();
	}

	@Test
	void withdrawalEligibilityReturnsAvailableCommissionBalance() {
		TrackerUser affiliate = affiliate();
		affiliate.completeAffiliateOnboarding("12 Main Road", "+27825550123", "https://paypal.me/affiliate");
		when(userRepository.findByUsername("affiliate")).thenReturn(Optional.of(affiliate));
		when(commissionRepository.findByAffiliate_IdAndStatusAndWithdrawalIsNullOrderByEarnedAtAsc(
				10L,
				AffiliateCommissionStatus.EARNED)).thenReturn(List.of(
						commission(affiliate, "180.00"),
						commission(affiliate, "230.00")));
		when(priceLocalizationService.localize(any(BigDecimal.class), eq(affiliate)))
				.thenReturn(money("410.00"));

		AffiliateWithdrawalEligibilityResponse response = service.withdrawalEligibility("affiliate");

		assertThat(response.availableAmount()).isEqualByComparingTo("410.00");
		assertThat(response.eligibleCommissionCount()).isEqualTo(2);
		assertThat(response.paypalLinkReady()).isTrue();
		assertThat(response.canWithdraw()).isTrue();
	}

	@Test
	void requestWithdrawalAssignsAvailableCommissions() {
		TrackerUser affiliate = affiliate();
		affiliate.completeAffiliateOnboarding("12 Main Road", "+27825550123", "https://paypal.me/affiliate");
		AffiliateCommission first = commission(affiliate, "180.00");
		AffiliateCommission second = commission(affiliate, "230.00");
		when(userRepository.findByUsername("affiliate")).thenReturn(Optional.of(affiliate));
		when(commissionRepository.findByAffiliate_IdAndStatusAndWithdrawalIsNullOrderByEarnedAtAsc(
				10L,
				AffiliateCommissionStatus.EARNED)).thenReturn(List.of(first, second));
		when(withdrawalRepository.save(any(AffiliateWithdrawal.class))).thenAnswer(invocation -> {
			AffiliateWithdrawal withdrawal = invocation.getArgument(0);
			ReflectionTestUtils.setField(withdrawal, "id", 88L);
			return withdrawal;
		});
		when(priceLocalizationService.localize(any(BigDecimal.class), eq(affiliate)))
				.thenReturn(money("410.00"));

		AffiliateWithdrawalResponse response = service.requestWithdrawal("affiliate");

		assertThat(response.id()).isEqualTo(88L);
		assertThat(response.amount()).isEqualByComparingTo("410.00");
		assertThat(response.status()).isEqualTo(AffiliateWithdrawalStatus.REQUESTED);
		assertThat(first.getStatus()).isEqualTo(AffiliateCommissionStatus.WITHDRAWAL_REQUESTED);
		assertThat(second.getStatus()).isEqualTo(AffiliateCommissionStatus.WITHDRAWAL_REQUESTED);
		assertThat(first.getWithdrawalId()).isEqualTo(88L);
		assertThat(second.getWithdrawalId()).isEqualTo(88L);
		verify(commissionRepository).saveAll(List.of(first, second));
	}

	@Test
	void requestWithdrawalRequiresPayPalLink() {
		TrackerUser affiliate = affiliate();
		when(userRepository.findByUsername("affiliate")).thenReturn(Optional.of(affiliate));

		assertThatThrownBy(() -> service.requestWithdrawal("affiliate"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("PayPal link is required before affiliate withdrawal");
	}

	private TrackerUser affiliate() {
		TrackerUser affiliate = new TrackerUser(
				"affiliate",
				"affiliate@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Affiliate));
		ReflectionTestUtils.setField(affiliate, "id", 10L);
		affiliate.activateAffiliateProfile("AFF-AFFILIATE-1234", "https://app.example/pricing?affiliateCode=AFF-AFFILIATE-1234", "https://qr.example");
		return affiliate;
	}

	private Business business(TrackerUser affiliate) {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 11L);
		business.assignAffiliate(affiliate, affiliate.getAffiliateCode());
		owner.setBusiness(business);
		return business;
	}

	private BusinessSubscription subscription(Business business, String amount) {
		BusinessSubscription subscription = new BusinessSubscription(
				business,
				BusinessPlan.PLUS,
				BillingInterval.MONTHLY,
				null,
				new BigDecimal(amount),
				"ZAR");
		ReflectionTestUtils.setField(subscription, "id", 44L);
		return subscription;
	}

	private AffiliateCommission commission(TrackerUser affiliate, String commissionAmount) {
		Business business = business(affiliate);
		BusinessSubscription subscription = subscription(business, "1000.00");
		return new AffiliateCommission(
				affiliate,
				business,
				subscription,
				new BigDecimal("1000.00"),
				new BigDecimal("18.00"),
				new BigDecimal(commissionAmount),
				"ZAR",
				LocalDateTime.of(2026, 6, 24, 12, 0));
	}

	private MoneyResponse money(String amount) {
		return new MoneyResponse(new BigDecimal(amount), SupportedCurrency.ZAR, "R", "R" + amount);
	}
}
