package com.king_sparkon_tracker.backend.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

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
class FinancialLedgerServiceTest {

	@Mock
	private FinancialJournalRepository journalRepository;
	@Mock
	private BusinessAccountLedgerEntryRepository legacyRepository;

	private FinancialLedgerService service;

	@BeforeEach
	void setUp() {
		service = new FinancialLedgerService(journalRepository, legacyRepository);
	}

	@Test
	void postedWalletCreditCreatesExactlyBalancedImmutableJournalOnce() {
		Business business = business(9L);
		BusinessAccountLedgerEntry entry = new BusinessAccountLedgerEntry(
				business,
				BusinessAccountEntryType.PRODUCT_SALE_CREDIT,
				BusinessAccountEntryStatus.POSTED,
				new BigDecimal("125.00"),
				new BigDecimal("125.00"),
				"STRIPE",
				"pi_123",
				null,
				"Product sale",
				"system");
		ReflectionTestUtils.setField(entry, "id", 44L);
		when(journalRepository.findByBusiness_IdAndSourceTypeAndSourceReference(
				9L, "BUSINESS_ACCOUNT_PRODUCT_SALE_CREDIT", "44")).thenReturn(Optional.empty());
		when(journalRepository.save(any(FinancialJournal.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FinancialJournal journal = service.postForBusinessAccountEntry(entry);

		assertThat(journal.getLines()).hasSize(2);
		BigDecimal debits = journal.getLines().stream()
				.filter(line -> line.getEntrySide() == LedgerSide.DEBIT)
				.map(FinancialLedgerLine::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal credits = journal.getLines().stream()
				.filter(line -> line.getEntrySide() == LedgerSide.CREDIT)
				.map(FinancialLedgerLine::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(debits).isEqualByComparingTo(credits);
		assertThat(journal.getImmutableHash()).hasSize(64);
		verify(journalRepository, times(1)).save(any(FinancialJournal.class));
	}

	private Business business(Long id) {
		TrackerUser owner = new TrackerUser("owner", "owner@example.com", "encoded", new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Store", owner);
		ReflectionTestUtils.setField(business, "id", id);
		return business;
	}
}
