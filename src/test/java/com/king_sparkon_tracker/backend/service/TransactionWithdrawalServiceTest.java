package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.OwnerPayoutAccount;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawalStatus;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.OwnerPayoutAccountRepository;
import com.king_sparkon_tracker.backend.repository.TransactionWithdrawalRepository;

@ExtendWith(MockitoExtension.class)
class TransactionWithdrawalServiceTest {

	@Mock
	private InventoryTransactionRepository transactionRepository;

	@Mock
	private OwnerPayoutAccountRepository payoutAccountRepository;

	@Mock
	private TransactionWithdrawalRepository withdrawalRepository;

	@Mock
	private PriceLocalizationService priceLocalizationService;

	@Mock
	private NotificationService notificationService;

	@Mock
	private TrackerUserService trackerUserService;

	private TransactionWithdrawalService withdrawalService;
	private TrackerUser owner;
	private TrackerUser worker;
	private Business business;

	@BeforeEach
	void setUp() {
		owner = user(1L, "owner", PrivilegeRole.Owner);
		worker = user(2L, "worker", PrivilegeRole.Worker);
		business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 3L);
		owner.setBusiness(business);
		worker.setBusiness(business);

		withdrawalService = new TransactionWithdrawalService(
				transactionRepository,
				payoutAccountRepository,
				withdrawalRepository,
				priceLocalizationService,
				notificationService,
				trackerUserService,
				new BigDecimal("1000"),
				7,
				new BigDecimal("6.5"),
				"http://localhost:3000/dashboard/owner/transactions/paypal/onboarding");
	}

	@Test
	void onboardPayPalAccountCreatesNormalizedOwnerPayoutAccount() {
		stubOwner();
		when(payoutAccountRepository.findByBusinessId(3L)).thenReturn(Optional.empty());
		when(payoutAccountRepository.save(any(OwnerPayoutAccount.class))).thenAnswer(invocation -> {
			OwnerPayoutAccount account = invocation.getArgument(0);
			ReflectionTestUtils.setField(account, "id", 5L);
			return account;
		});

		OwnerPayPalAccountResponse response = withdrawalService.onboardPayPalAccount(
				new OwnerPayPalAccountOnboardingRequest(
						" OWNER@PAYPAL.COM ",
						"https://app.example/paypal/callback"),
				"owner");

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.ownerId()).isEqualTo(1L);
		assertThat(response.businessId()).isEqualTo(3L);
		assertThat(response.paypalEmail()).isEqualTo("owner@paypal.com");
		assertThat(response.onboardingUrl()).contains("businessId=3");
		assertThat(response.onboardingUrl()).contains("token=");
	}

	@Test
	void eligibilityAppliesWebsiteWithdrawalFeeAndMinimumRules() {
		stubOwner();
		when(payoutAccountRepository.findByBusinessId(3L)).thenReturn(Optional.of(ownerAccount()));
		when(transactionRepository.findByBusiness_IdAndTypeAndPaymentTypeAndPaymentStatusAndTransactionWithdrawalIdIsNullAndDateLessThanEqualOrderByDateAsc(
				eq(3L),
				eq(TransactionType.SELL),
				eq(TransactionPaymentType.WEBSITE_PAYMENT),
				eq(TransactionPaymentStatus.PAID),
				any()))
				.thenReturn(List.of(
						websitePaymentTransaction(10L, "700.00", 1),
						websitePaymentTransaction(11L, "250.00", 2)));
		stubMoney("1200.00", "R1,200.00");
		stubMoney("78.00", "R78.00");
		stubMoney("1122.00", "R1,122.00");
		stubMoney("1000.00", "R1,000.00");

		TransactionWithdrawalEligibilityResponse response = withdrawalService.eligibility("owner");

		assertThat(response.ownerId()).isEqualTo(1L);
		assertThat(response.businessId()).isEqualTo(3L);
		assertThat(response.grossAmount()).isEqualByComparingTo("1200.00");
		assertThat(response.feeAmount()).isEqualByComparingTo("78.00");
		assertThat(response.feePercent()).isEqualByComparingTo("6.50");
		assertThat(response.availableAmount()).isEqualByComparingTo("1122.00");
		assertThat(response.eligibleTransactionCount()).isEqualTo(2);
		assertThat(response.paypalAccountReady()).isTrue();
		assertThat(response.canWithdraw()).isTrue();
	}

	@Test
	void requestWithdrawalAssignsEligibleTransactionsAndReturnsBreakdown() {
		stubOwner();
		List<InventoryTransaction> transactions = List.of(
				websitePaymentTransaction(10L, "800.00", 1),
				websitePaymentTransaction(11L, "250.00", 2));
		when(payoutAccountRepository.findByBusinessId(3L)).thenReturn(Optional.of(ownerAccount()));
		when(transactionRepository.findByBusiness_IdAndTypeAndPaymentTypeAndPaymentStatusAndTransactionWithdrawalIdIsNullAndDateLessThanEqualOrderByDateAsc(
				eq(3L),
				eq(TransactionType.SELL),
				eq(TransactionPaymentType.WEBSITE_PAYMENT),
				eq(TransactionPaymentStatus.PAID),
				any()))
				.thenReturn(transactions);
		when(withdrawalRepository.save(any(TransactionWithdrawal.class))).thenAnswer(invocation -> {
			TransactionWithdrawal withdrawal = invocation.getArgument(0);
			ReflectionTestUtils.setField(withdrawal, "id", 77L);
			return withdrawal;
		});
		stubMoney("1300.00", "R1,300.00");
		stubMoney("84.50", "R84.50");
		stubMoney("1215.50", "R1,215.50");

		TransactionWithdrawalResponse response = withdrawalService.requestWithdrawal(null, "owner");

		assertThat(response.id()).isEqualTo(77L);
		assertThat(response.ownerId()).isEqualTo(1L);
		assertThat(response.businessId()).isEqualTo(3L);
		assertThat(response.grossAmount()).isEqualByComparingTo("1300.00");
		assertThat(response.feeAmount()).isEqualByComparingTo("84.50");
		assertThat(response.amount()).isEqualByComparingTo("1215.50");
		assertThat(response.transactionCount()).isEqualTo(2);
		assertThat(response.status()).isEqualTo(TransactionWithdrawalStatus.REQUESTED);
		assertThat(transactions.getFirst().getTransactionWithdrawalId()).isEqualTo(77L);
		assertThat(transactions.get(1).getTransactionWithdrawalId()).isEqualTo(77L);
		verify(transactionRepository).saveAll(transactions);
		verify(notificationService).logTransactionWithdrawalRequested(any(TransactionWithdrawal.class));
	}

	@Test
	void requestWithdrawalRequiresOwnerPayPalOnboarding() {
		stubOwner();
		when(payoutAccountRepository.findByBusinessId(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> withdrawalService.requestWithdrawal(null, "owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Owner PayPal payout account onboarding is required before transaction withdrawal");
	}

	@Test
	void requestWithdrawalRejectsIneligibleSelectedTransactionIds() {
		stubOwner();
		when(payoutAccountRepository.findByBusinessId(3L)).thenReturn(Optional.of(ownerAccount()));
		when(transactionRepository.findEligibleWebsitePaymentWithdrawalsByIds(
				eq(3L),
				eq(List.of(10L, 11L)),
				eq(TransactionType.SELL),
				eq(TransactionPaymentType.WEBSITE_PAYMENT),
				eq(TransactionPaymentStatus.PAID),
				any()))
				.thenReturn(List.of(websitePaymentTransaction(10L, "1200.00", 1)));

		assertThatThrownBy(() -> withdrawalService.requestWithdrawal(new TransactionWithdrawalRequest(List.of(10L, 11L)), "owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("One or more website payment transactions are not eligible for withdrawal");
	}

	private void stubOwner() {
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
	}

	private OwnerPayoutAccount ownerAccount() {
		return new OwnerPayoutAccount(1L, 3L, "owner@paypal.com", "token", "https://app.example/onboard");
	}

	private InventoryTransaction websitePaymentTransaction(Long id, String unitPrice, int quantity) {
		InventoryTransaction transaction = new InventoryTransaction(TransactionType.SELL, worker, owner, business);
		ReflectionTestUtils.setField(transaction, "id", id);
		transaction.setDate(LocalDateTime.now().minusDays(8));
		transaction.markWebsitePaymentPaid("pi_" + id);
		transaction.addItem(new TransactionItem(product(id), quantity, new BigDecimal(unitPrice)));
		return transaction;
	}

	private Product product(Long id) {
		Product product = new Product("Product " + id, ProductCategory.NonAlcohol, new BigDecimal("10.00"), 10, false);
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private void stubMoney(String amount, String formatted) {
		BigDecimal value = new BigDecimal(amount);
		lenient().when(priceLocalizationService.base(value))
				.thenReturn(new MoneyResponse(value, SupportedCurrency.ZAR, "R", formatted));
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
