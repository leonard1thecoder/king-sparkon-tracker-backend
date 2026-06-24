package com.king_sparkon_tracker.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.king_sparkon_tracker.backend.dto.CreateTransactionRequest;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.service.SubscriberService;
import com.king_sparkon_tracker.backend.service.TransactionService;

class TransactionControllerPaymentContactTest {

	private TransactionService transactionService;
	private SubscriberService subscriberService;
	private InventoryTransactionRepository transactionRepository;
	private TransactionController controller;
	private Principal principal;

	@BeforeEach
	void setUp() {
		transactionService = mock(TransactionService.class);
		subscriberService = mock(SubscriberService.class);
		transactionRepository = mock(InventoryTransactionRepository.class);
		controller = new TransactionController(transactionService, subscriberService, transactionRepository);
		principal = () -> "owner";
	}

	@Test
	void positiveWebsitePaymentStoresAndSubscribesPaymentContact() {
		CreateTransactionRequest request = new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.WEBSITE_PAYMENT,
				"customer@example.com",
				" +27 82 123 4567 ",
				2L,
				1L,
				List.of());
		InventoryTransaction transaction = transaction();
		transaction.prepareWebsitePayment("customer@example.com");
		when(transactionService.createTransaction(request, "owner")).thenReturn(transaction);
		when(transactionRepository.save(transaction)).thenReturn(transaction);

		controller.createTransaction(request, principal);

		verify(subscriberService).subscribeWebsitePaymentClient("+27 82 123 4567");
		assertThat(transaction.getPaymentContact()).isEqualTo("+27 82 123 4567");
		verify(transactionRepository).save(transaction);
	}

	@Test
	void negativeCashPaymentDoesNotSubscribeOrPersistPaymentContact() {
		CreateTransactionRequest request = new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				"customer@example.com",
				"+27821234567",
				2L,
				1L,
				List.of());
		InventoryTransaction transaction = transaction();
		when(transactionService.createTransaction(request, "owner")).thenReturn(transaction);

		controller.createTransaction(request, principal);

		verify(subscriberService, never()).subscribeWebsitePaymentClient("+27821234567");
		verify(transactionRepository, never()).save(transaction);
	}

	private InventoryTransaction transaction() {
		TrackerUser owner = new TrackerUser("owner", "owner@example.com", "encoded", new Privilege(PrivilegeRole.Owner));
		TrackerUser worker = new TrackerUser("worker", "worker@example.com", "encoded", new Privilege(PrivilegeRole.Worker));
		Business business = new Business("Spark Store", owner);
		owner.setBusiness(business);
		worker.setBusiness(business);
		return new InventoryTransaction(TransactionType.SELL, worker, owner, business);
	}
}
