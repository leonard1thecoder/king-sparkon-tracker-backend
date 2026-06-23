package com.king_sparkon_tracker.backend.tips;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.dto.PayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TipWithdrawalStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.WorkerPayoutAccount;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.repository.TipWithdrawalRepository;
import com.king_sparkon_tracker.backend.repository.WorkerPayoutAccountRepository;
import com.king_sparkon_tracker.backend.service.NotificationService;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.TipWithdrawalService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

@ExtendWith(MockitoExtension.class)
class TipWithdrawalServiceTest {

	@Mock
	private TipRepository tipRepository;

	@Mock
	private WorkerPayoutAccountRepository payoutAccountRepository;

	@Mock
	private TipWithdrawalRepository withdrawalRepository;

	@Mock
	private PriceLocalizationService priceLocalizationService;

	@Mock
	private NotificationService notificationService;

	@Mock
	private TrackerUserService trackerUserService;

	private TipWithdrawalService withdrawalService;
	private TrackerUser owner;
	private TrackerUser worker;

	@BeforeEach
	void setUp() {
		owner = user(1L, "owner", PrivilegeRole.Owner);
		worker = user(10L, "worker", PrivilegeRole.Worker);
		withdrawalService = new TipWithdrawalService(
				tipRepository,
				payoutAccountRepository,
				withdrawalRepository,
				priceLocalizationService,
				notificationService,
				trackerUserService,
				new BigDecimal("1000"),
				7,
				"http://localhost:3000/dashboard/owner/paypal/onboarding",
				new BigDecimal("8.5"));
	}

	@Test
	void onboardPayPalAccountCreatesNormalizedPayoutAccount() {
		stubOwnerAndWorker();
		when(payoutAccountRepository.findByWorker_Id(10L)).thenReturn(Optional.empty());
		when(payoutAccountRepository.save(any(WorkerPayoutAccount.class))).thenAnswer(invocation -> {
			WorkerPayoutAccount account = invocation.getArgument(0);
			ReflectionTestUtils.setField(account, "id", 5L);
			return account;
		});

		PayPalAccountResponse response = withdrawalService.onboardPayPalAccount(new PayPalAccountOnboardingRequest(
				10L,
				" WORKER@PAYPAL.COM ",
				"https://app.example/paypal/callback"),
				"owner");

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.workerId()).isEqualTo(10L);
		assertThat(response.ownerId()).isEqualTo(1L);
		assertThat(response.paypalEmail()).isEqualTo("worker@paypal.com");
		assertThat(response.onboardingUrl()).contains("workerId=10");
		assertThat(response.onboardingUrl()).contains("token=");
	}

	@Test
	void requestWithdrawalAssignsEligibleTipsAndReturnsRequestedWithdrawal() {
		stubOwnerAndWorker();
		WorkerPayoutAccount account = new WorkerPayoutAccount(
				worker,
				owner,
				"worker@paypal.com",
				"token",
				"https://app.example/onboard");
		Tip firstTip = paidTip(10L, "800.00");
		Tip secondTip = paidTip(10L, "400.00");
		when(payoutAccountRepository.findByWorker_Id(10L)).thenReturn(Optional.of(account));
		when(tipRepository.findByWorker_IdAndStatusAndWithdrawalIsNullAndCreatedLessThanEqualOrderByCreatedDesc(
				org.mockito.ArgumentMatchers.eq(10L),
				org.mockito.ArgumentMatchers.eq(TipStatus.PAID),
				any()))
				.thenReturn(List.of(firstTip, secondTip));
		when(withdrawalRepository.save(any(TipWithdrawal.class))).thenAnswer(invocation -> {
			TipWithdrawal withdrawal = invocation.getArgument(0);
			ReflectionTestUtils.setField(withdrawal, "id", 77L);
			return withdrawal;
		});
		when(priceLocalizationService.base(new BigDecimal("1098.00")))
				.thenReturn(new MoneyResponse(new BigDecimal("1098.00"), SupportedCurrency.ZAR, "R", "R1,098.00"));

		WithdrawalResponse response = withdrawalService.requestWithdrawal(new WithdrawalRequest(10L), "owner");

		assertThat(response.id()).isEqualTo(77L);
		assertThat(response.ownerId()).isEqualTo(1L);
		assertThat(response.amount()).isEqualByComparingTo("1098.00");
		assertThat(response.status()).isEqualTo(TipWithdrawalStatus.REQUESTED);
		assertThat(response.tipCount()).isEqualTo(2);
		assertThat(firstTip.getWithdrawalId()).isEqualTo(77L);
		assertThat(secondTip.getWithdrawalId()).isEqualTo(77L);
		verify(notificationService).logWithdrawalRequested(any(TipWithdrawal.class));
	}

	@Test
	void requestWithdrawalRequiresPayPalOnboarding() {
		stubOwnerAndWorker();
		when(payoutAccountRepository.findByWorker_Id(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> withdrawalService.requestWithdrawal(new WithdrawalRequest(10L), "owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("PayPal payout account onboarding is required before withdrawal");
	}

	@Test
	void requestWithdrawalRequiresMinimumAvailableBalance() {
		stubOwnerAndWorker();
		WorkerPayoutAccount account = new WorkerPayoutAccount(
				worker,
				owner,
				"worker@paypal.com",
				"token",
				"https://app.example/onboard");
		when(payoutAccountRepository.findByWorker_Id(10L)).thenReturn(Optional.of(account));
		when(tipRepository.findByWorker_IdAndStatusAndWithdrawalIsNullAndCreatedLessThanEqualOrderByCreatedDesc(
				org.mockito.ArgumentMatchers.eq(10L),
				org.mockito.ArgumentMatchers.eq(TipStatus.PAID),
				any()))
				.thenReturn(List.of(paidTip(10L, "500.00")));

		assertThatThrownBy(() -> withdrawalService.requestWithdrawal(new WithdrawalRequest(10L), "owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Available tip balance must be at least R1000.00 before withdrawal");
	}

	private Tip paidTip(Long workerId, String amount) {
		Tip tip = new Tip(worker, new BigDecimal(amount));
		tip.markPaid();
		return tip;
	}

	private void stubOwnerAndWorker() {
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
		when(trackerUserService.getUserById(10L, "owner")).thenReturn(worker);
	}

	private TrackerUser user(Long id, String username, PrivilegeRole role) {
		TrackerUser user = new TrackerUser(
				username,
				username + "@example.com",
				"encoded",
				new Privilege(role));
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
