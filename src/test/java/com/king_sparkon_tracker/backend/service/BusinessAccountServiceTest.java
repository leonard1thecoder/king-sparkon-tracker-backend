package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryStatus;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessAccountLedgerEntryRepository;

@ExtendWith(MockitoExtension.class)
class BusinessAccountServiceTest {

	@Mock
	private BusinessAccountLedgerEntryRepository ledgerRepository;

	@Mock
	private TrackerUserService trackerUserService;

	@Mock
	private StripeService stripeService;

	private BusinessAccountService service;

	@BeforeEach
	void setUp() {
		service = new BusinessAccountService(ledgerRepository, trackerUserService, stripeService);
	}

	@Test
	void debitPromotionRejectsInsufficientBalance() {
		Business business = business();
		when(ledgerRepository.lockPostedEntries(1L, BusinessAccountEntryStatus.POSTED)).thenReturn(List.of());
		when(ledgerRepository.sumAmountByBusinessIdAndStatus(1L, BusinessAccountEntryStatus.POSTED)).thenReturn(new BigDecimal("50.00"));

		assertThatThrownBy(() -> service.debitPromotion(
				business,
				new BigDecimal("100.00"),
				BusinessAccountEntryType.TICKET_PROMOTION_DEBIT,
				"Ticket boost",
				"owner"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("balance is too low");
	}

	@Test
	void debitPromotionPostsNegativeLedgerEntryAndBalanceAfter() {
		Business business = business();
		when(ledgerRepository.lockPostedEntries(1L, BusinessAccountEntryStatus.POSTED)).thenReturn(List.of());
		when(ledgerRepository.sumAmountByBusinessIdAndStatus(1L, BusinessAccountEntryStatus.POSTED)).thenReturn(new BigDecimal("250.00"));
		when(ledgerRepository.save(any(BusinessAccountLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BusinessAccountLedgerEntry entry = service.debitPromotion(
				business,
				new BigDecimal("100.00"),
				BusinessAccountEntryType.PROMOTION_DEBIT,
				"Owner campaign",
				"owner");

		assertThat(entry.getStatus()).isEqualTo(BusinessAccountEntryStatus.POSTED);
		assertThat(entry.getEntryType()).isEqualTo(BusinessAccountEntryType.PROMOTION_DEBIT);
		assertThat(entry.getAmount()).isEqualByComparingTo("-100.00");
		assertThat(entry.getBalanceAfter()).isEqualByComparingTo("150.00");
	}

	private Business business() {
		TrackerUser owner = new TrackerUser("owner", "owner@example.com", "secret", new Privilege(PrivilegeRole.Owner));
		ReflectionTestUtils.setField(owner, "id", 7L);
		Business business = new Business("Sparkon Events", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		return business;
	}
}
