package com.king_sparkon_tracker.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.CreateTransactionRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.service.TransactionService;

class TransactionControllerTest {

	private TransactionService transactionService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		transactionService = mock(TransactionService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new TransactionController(transactionService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void createTransactionReturnsCreatedTransaction() throws Exception {
		when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq("worker")))
				.thenReturn(transaction(TransactionType.SELL));

		mockMvc.perform(post("/api/transactions")
				.principal(() -> "worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "type": "SELL",
						  "paymentType": "CASH",
						  "employeeId": 2,
						  "ownerId": 1,
						  "items": [
						    { "productId": 9, "quantity": 2, "unitPrice": 20.50, "barcodes": ["6001", "6002"] }
						  ]
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("SELL"))
				.andExpect(jsonPath("$.paymentType").value("CASH"))
				.andExpect(jsonPath("$.paymentStatus").value("NOT_REQUIRED"))
				.andExpect(jsonPath("$.paymentUrl").doesNotExist())
				.andExpect(jsonPath("$.totalAmount").value(41.00))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andExpect(jsonPath("$.items[0].productName").value("Beer"))
				.andExpect(jsonPath("$.items[0].barcodes[0]").value("6001"));
	}

	@Test
	void createWebsitePaymentTransactionReturnsPaymentFields() throws Exception {
		when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq("worker")))
				.thenReturn(websitePaymentTransaction());

		mockMvc.perform(post("/api/transactions")
				.principal(() -> "worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "type": "SELL",
						  "paymentType": "WEBSITE_PAYMENT",
						  "paymentEmail": "customer@example.com",
						  "employeeId": 2,
						  "ownerId": 1,
						  "items": [
						    { "productId": 9, "quantity": 2, "unitPrice": 20.50, "barcodes": ["6001", "6002"] }
						  ]
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.paymentType").value("WEBSITE_PAYMENT"))
				.andExpect(jsonPath("$.paymentStatus").value("PENDING"))
				.andExpect(jsonPath("$.paymentReference").value("plink_123"))
				.andExpect(jsonPath("$.paymentUrl").value("https://pay.stripe.com/plink_123"))
				.andExpect(jsonPath("$.paymentEmail").value("customer@example.com"))
				.andExpect(jsonPath("$.totalAmount").value(41.00));
	}

	@Test
	void createTransactionMapsBadRequest() throws Exception {
		when(transactionService.createTransaction(any(CreateTransactionRequest.class), eq("worker")))
				.thenThrow(new IllegalArgumentException("Not enough stock for product: Beer"));

		mockMvc.perform(post("/api/transactions")
				.principal(() -> "worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "type": "SELL",
						  "paymentType": "CASH",
						  "employeeId": 2,
						  "ownerId": 1,
						  "items": [
						    { "productId": 9, "quantity": 999, "unitPrice": 20.50 }
						  ]
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Not enough stock for product: Beer"));
	}

	@Test
	void listTransactionsReturnsTransactions() throws Exception {
		when(transactionService.listTransactions(PageRequest.of(0, 20), "owner")).thenReturn(new PageImpl<>(
				java.util.List.of(transaction(TransactionType.BUY)),
				PageRequest.of(0, 20),
				1));

		mockMvc.perform(get("/api/transactions").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].type").value("BUY"))
				.andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
				.andExpect(jsonPath("$.content[0].items[0].barcodes[0]").value("6001"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getTransactionByIdReturnsTransaction() throws Exception {
		when(transactionService.getTransactionById(7L, "owner")).thenReturn(transaction(TransactionType.SELL));

		mockMvc.perform(get("/api/transactions/7").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("SELL"));
	}

	@Test
	void getTransactionByIdMapsMissingTransactionToNotFound() throws Exception {
		when(transactionService.getTransactionById(7L, "owner"))
				.thenThrow(new ResourceNotFoundException("Transaction not found: 7"));

		mockMvc.perform(get("/api/transactions/7").principal(() -> "owner"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Transaction not found: 7"));
	}

	private InventoryTransaction transaction(TransactionType type) {
		TrackerUser employee = new TrackerUser(
				"worker",
				"worker@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Worker));
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@kingsparkon.co.za",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Product product = new Product("Beer", "6001", ProductCategory.Alcohol, new BigDecimal("20.50"), 10);
		ReflectionTestUtils.setField(product, "id", 9L);
		InventoryTransaction transaction = new InventoryTransaction(type, employee, owner);
		if (type == TransactionType.SELL) {
			transaction.markOfflinePayment(TransactionPaymentType.CASH, null);
		}
		transaction.addItem(new TransactionItem(product, 2, new BigDecimal("20.50"), java.util.List.of("6001", "6002")));
		return transaction;
	}

	private InventoryTransaction websitePaymentTransaction() {
		InventoryTransaction transaction = transaction(TransactionType.SELL);
		transaction.markWebsitePaymentPending(
				"customer@example.com",
				"plink_123",
				"https://pay.stripe.com/plink_123");
		return transaction;
	}
}
