package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.CreateTransactionRequest;
import com.king_sparkon_tracker.backend.dto.TransactionItemRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.service.StripeService.CreatedTransactionPaymentLink;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private InventoryTransactionRepository transactionRepository;

	@Mock
	private ProductService productService;

	@Mock
	private TrackerUserService userService;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private ProductBarcodeRepository productBarcodeRepository;

	@Mock
	private AppEmailService appEmailService;

	@Mock
	private StripeService stripeService;

	private TransactionService transactionService;
	private Business business;

	@BeforeEach
	void setUp() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		lenient().when(userService.businessForActor("worker")).thenReturn(business);
		transactionService = transactionServiceAt(LocalDateTime.of(2026, 6, 1, 12, 0));
	}

	@Test
	void createTransactionRecordsItemsAndAppliesStockMovement() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product("Beer", ProductCategory.Alcohol);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByBarcodeIn(List.of("6001", "6002", "6003")))
				.thenReturn(productBarcodes(product, "6001", "6002", "6003"));
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 3, new BigDecimal("18.50"), List.of("6001", "6002", "6003")))),
				"worker");

		assertThat(result.getType()).isEqualTo(TransactionType.SELL);
		assertThat(result.getPaymentType()).isEqualTo(TransactionPaymentType.CASH);
		assertThat(result.getPaymentStatus()).isEqualTo(TransactionPaymentStatus.NOT_REQUIRED);
		assertThat(result.getPaymentEmail()).isNull();
		assertThat(result.getPaymentUrl()).isNull();
		assertThat(result.getEmployee()).isSameAs(employee);
		assertThat(result.getOwner()).isSameAs(owner);
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().getProduct()).isSameAs(product);
		assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(3);
		assertThat(result.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("18.50");
		assertThat(result.getItems().getFirst().getBarcodes()).containsExactly("6001", "6002", "6003");
		assertThat(result.getTotalAmount()).isEqualByComparingTo("55.50");
		verify(productService).applyStockMovement(product, TransactionType.SELL, 3);
		verify(appEmailService).sendTransactionCreatedOwnerEmail(result);
		verify(stripeService, never()).createTransactionPaymentLink(any(InventoryTransaction.class));
		verify(appEmailService, never()).sendTransactionWebsitePaymentEmail(any(InventoryTransaction.class));
	}

	@Test
	void createTransactionCreatesWebsitePaymentLinkAndEmailsCustomerForFullBasket() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product beer = product(9L, "Beer", ProductCategory.Alcohol, false);
		Product water = product(12L, "Water", ProductCategory.NonAlcohol, false);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(beer);
		when(productService.getProductForStockUpdate(12L, 1L)).thenReturn(water);
		when(productBarcodeRepository.findByBarcodeIn(List.of("6001", "6002")))
				.thenReturn(productBarcodes(beer, "6001", "6002"));
		when(productBarcodeRepository.findByBarcodeIn(List.of("7001")))
				.thenReturn(productBarcodes(water, "7001"));
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> {
					InventoryTransaction transaction = invocation.getArgument(0);
					ReflectionTestUtils.setField(transaction, "id", 7L);
					return transaction;
				});
		when(stripeService.createTransactionPaymentLink(any(InventoryTransaction.class)))
				.thenReturn(new CreatedTransactionPaymentLink("plink_123", "https://pay.stripe.com/plink_123"));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.WEBSITE_PAYMENT,
				" Customer@Example.com ",
				2L,
				1L,
				List.of(
						new TransactionItemRequest(9L, 2, new BigDecimal("20.50"), List.of("6001", "6002")),
						new TransactionItemRequest(12L, 1, new BigDecimal("12.00"), List.of("7001")))),
				"worker");

		assertThat(result.getPaymentType()).isEqualTo(TransactionPaymentType.WEBSITE_PAYMENT);
		assertThat(result.getPaymentStatus()).isEqualTo(TransactionPaymentStatus.PENDING);
		assertThat(result.getPaymentEmail()).isEqualTo("customer@example.com");
		assertThat(result.getPaymentReference()).isEqualTo("plink_123");
		assertThat(result.getPaymentUrl()).isEqualTo("https://pay.stripe.com/plink_123");
		assertThat(result.getTotalAmount()).isEqualByComparingTo("53.00");
		assertThat(result.getItems()).hasSize(2);
		verify(stripeService).createTransactionPaymentLink(result);
		verify(appEmailService).sendTransactionWebsitePaymentEmail(result);
	}

	@Test
	void createTransactionRejectsWebsitePaymentWithoutPaymentEmail() {
		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.WEBSITE_PAYMENT,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("6001")))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Website payment requires paymentEmail");
	}

	@Test
	void createCashTransactionUsesReturnableReferenceEmailWithoutCustomerPaymentEmail() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = returnableNightShiftProduct();
		List<ProductBarcode> barcodes = productBarcodes(product, "6001");
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByBarcodeIn(List.of("6001"))).thenReturn(barcodes);
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("6001"), "Customer@Example.com"))),
				"worker");

		assertThat(result.getPaymentType()).isEqualTo(TransactionPaymentType.CASH);
		assertThat(result.getPaymentEmail()).isNull();
		assertThat(barcodes.getFirst().getReferenceEmail()).isEqualTo("customer@example.com");
		verify(stripeService, never()).createTransactionPaymentLink(any(InventoryTransaction.class));
		verify(appEmailService, never()).sendTransactionWebsitePaymentEmail(any(InventoryTransaction.class));
	}

	@Test
	void createCashTransactionRejectsReturnableSaleWithoutReferenceEmail() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = returnableNightShiftProduct();
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("6001")))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Returnable barcode reference email is required");
	}

	@Test
	void createCashTransactionRejectsCellphoneBarcodeReference() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = returnableNightShiftProduct();
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("6001"), "0821234567"))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Reference email must be a valid email address");
	}

	@Test
	void createTransactionSendsWorkerCopyOnlyWhenActorIsDifferentWorker() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product("Beer", ProductCategory.Alcohol);
		when(userService.businessForActor("owner")).thenReturn(business);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.BUY,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null))),
				"owner");

		verify(appEmailService).sendTransactionCreatedOwnerEmail(result);
		verify(appEmailService).sendTransactionCreatedWorkerEmail(result);
	}

	@Test
	void createTransactionContinuesWhenOwnerEmailFails() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product("Beer", ProductCategory.Alcohol);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(appEmailService.sendTransactionCreatedOwnerEmail(any(InventoryTransaction.class)))
				.thenThrow(new IllegalStateException("smtp down"));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.BUY,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null))),
				"worker");

		assertThat(result.getItems()).hasSize(1);
		verify(auditLogService).record(
				eq("TRANSACTION_CREATED"),
				eq("InventoryTransaction"),
				any(),
				eq("worker"),
				eq("Transaction type: BUY, items: 1"),
				eq(business));
	}

	@Test
	void createTransactionRejectsSellPayloadWithoutScannedBarcodes() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product("Beer", ProductCategory.Alcohol);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
						TransactionType.SELL,
						TransactionPaymentType.CASH,
						null,
						2L,
						1L,
						List.of(new TransactionItemRequest(9L, 2, null))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("SELL transaction requires scanned barcodes");
	}

	@Test
	void createTransactionUsesProductPriceWhenUnitPriceIsMissing() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product("Beer", ProductCategory.Alcohol);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.BUY,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 3, null))),
				"worker");

		assertThat(result.getItems().getFirst().getUnitPrice()).isEqualByComparingTo(product.getPrice());
		verify(productService).applyStockMovement(product, TransactionType.BUY, 3);
	}

	@Test
	void createTransactionAddsReturnableAndEarlyMorningChargesOnSell() {
		transactionService = transactionServiceAt(LocalDateTime.of(2026, 6, 1, 8, 0));
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = returnableNightShiftProduct();
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByBarcodeIn(List.of("6001"))).thenReturn(productBarcodes(product, "6001"));
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				"customer@example.com",
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("6001")))),
				"worker");

		assertThat(result.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("26.00");
		verify(productService).applyStockMovement(product, TransactionType.SELL, 1);
	}

	@Test
	void createTransactionUsesConfiguredReturnableChargeOutsideNightShift() {
		transactionService = transactionServiceAt(LocalDateTime.of(2026, 6, 5, 12, 0));
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = returnableNightShiftProduct();
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByBarcodeIn(List.of("6001"))).thenReturn(productBarcodes(product, "6001"));
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				"customer@example.com",
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("6001")))),
				"worker");

		assertThat(result.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("23.00");
		verify(productService).applyStockMovement(product, TransactionType.SELL, 1);
	}

	@Test
	void createTransactionLocksProductsInIdOrderBeforeApplyingMovements() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product lowerProduct = product("Water", ProductCategory.NonAlcohol);
		Product higherProduct = product("Beer", ProductCategory.Alcohol);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(3L, 1L)).thenReturn(lowerProduct);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(higherProduct);
		when(productBarcodeRepository.findByBarcodeIn(List.of("9001"))).thenReturn(productBarcodes(higherProduct, "9001"));
		when(productBarcodeRepository.findByBarcodeIn(List.of("3001"))).thenReturn(productBarcodes(lowerProduct, "3001"));
		when(transactionRepository.save(any(InventoryTransaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(
						new TransactionItemRequest(9L, 1, null, List.of("9001")),
						new TransactionItemRequest(3L, 1, null, List.of("3001")))),
				"worker");

		InOrder inOrder = inOrder(productService);
		inOrder.verify(productService).getProductForStockUpdate(3L, 1L);
		inOrder.verify(productService).getProductForStockUpdate(9L, 1L);
		assertThat(result.getItems().getFirst().getProduct()).isSameAs(higherProduct);
		assertThat(result.getItems().get(1).getProduct()).isSameAs(lowerProduct);
	}

	@Test
	void createTransactionRejectsSellBarcodeCountMismatch() {
		when(userService.getUserById(2L)).thenReturn(user("worker", PrivilegeRole.Worker));
		when(userService.getUserById(1L)).thenReturn(user("owner", PrivilegeRole.Owner));
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product("Beer", ProductCategory.Alcohol));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 2, null, List.of("6001")))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("SELL transaction barcode count must match quantity");
	}

	@Test
	void createTransactionRejectsBarcodeFromDifferentProduct() {
		Product product = product("Beer", ProductCategory.Alcohol);
		Product otherProduct = product(12L, "Water", ProductCategory.NonAlcohol, false);
		when(userService.getUserById(2L)).thenReturn(user("worker", PrivilegeRole.Worker));
		when(userService.getUserById(1L)).thenReturn(user("owner", PrivilegeRole.Owner));
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByBarcodeIn(List.of("7001"))).thenReturn(productBarcodes(otherProduct, "7001"));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("7001")))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Every SELL barcode must be registered to the selected product");
	}

	@Test
	void createTransactionRejectsEmptyItems() {
		assertThatThrownBy(() -> transactionService.createTransaction(
				new CreateTransactionRequest(TransactionType.BUY, 2L, 1L, List.of()), "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Transaction must contain at least one item");
	}

	@Test
	void createTransactionRejectsNonWorkerEmployee() {
		when(userService.getUserById(1L)).thenReturn(user("owner", PrivilegeRole.Owner));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				1L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Employee must have role Worker");
	}

	@Test
	void createTransactionRejectsNonOwnerOwner() {
		when(userService.getUserById(2L)).thenReturn(user("worker", PrivilegeRole.Worker));
		when(userService.getUserById(3L)).thenReturn(user("other-worker", PrivilegeRole.Worker));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				3L,
				List.of(new TransactionItemRequest(9L, 1, null))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Owner must have role Owner");
	}

	@Test
	void listTransactionsReturnsAllTransactions() {
		List<InventoryTransaction> transactions = List.of(new InventoryTransaction(
				TransactionType.BUY,
				user("worker", PrivilegeRole.Worker),
				user("owner", PrivilegeRole.Owner)));
		when(transactionRepository.findAll()).thenReturn(transactions);

		assertThat(transactionService.listTransactions()).containsExactlyElementsOf(transactions);
	}

	@Test
	void getTransactionByIdReturnsTransaction() {
		InventoryTransaction transaction = new InventoryTransaction(
				TransactionType.BUY,
				user("worker", PrivilegeRole.Worker),
				user("owner", PrivilegeRole.Owner));
		when(transactionRepository.findById(7L)).thenReturn(Optional.of(transaction));

		assertThat(transactionService.getTransactionById(7L)).isSameAs(transaction);
	}

	@Test
	void getTransactionByIdThrowsWhenMissing() {
		when(transactionRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.getTransactionById(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Transaction not found: 7");
	}

	private Product product(String name, ProductCategory category) {
		return product(9L, name, category, false);
	}

	private Product product(String name, ProductCategory category, boolean bottleReturnable) {
		return product(9L, name, category, bottleReturnable);
	}

	private Product product(Long id, String name, ProductCategory category, boolean bottleReturnable) {
		Product product = new Product(name, category, new BigDecimal("20.00"), 10, bottleReturnable);
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private Product returnableNightShiftProduct() {
		Product product = new Product(
				"Beer",
				ProductCategory.Alcohol,
				new BigDecimal("20.00"),
				10,
				true,
				new BigDecimal("3.00"),
				true,
				new BigDecimal("3.00"),
				LocalTime.of(0, 0),
				LocalTime.of(9, 30),
				null);
		ReflectionTestUtils.setField(product, "id", 9L);
		return product;
	}

	private List<ProductBarcode> productBarcodes(Product product, String... barcodes) {
		return java.util.Arrays.stream(barcodes)
				.map(barcode -> {
					ProductBarcode productBarcode = new ProductBarcode(barcode);
					productBarcode.setProduct(product);
					return productBarcode;
				})
				.toList();
	}

	private TransactionService transactionServiceAt(LocalDateTime dateTime) {
		ZoneId zone = ZoneId.of("Africa/Johannesburg");
		Clock fixedClock = Clock.fixed(dateTime.atZone(zone).toInstant(), zone);
		return new TransactionService(
				transactionRepository,
				productService,
				userService,
				auditLogService,
				new ProductPricingService(fixedClock),
				productBarcodeRepository,
				appEmailService,
				stripeService);
	}

	private TrackerUser user(String username, PrivilegeRole role) {
		TrackerUser user = new TrackerUser(username, username + "@example.com", "encoded", new Privilege(role));
		user.setBusiness(business);
		return user;
	}
}
