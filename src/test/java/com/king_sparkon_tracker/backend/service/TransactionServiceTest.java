package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
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

import com.king_sparkon_tracker.backend.dto.CreateTransactionRequest;
import com.king_sparkon_tracker.backend.dto.TransactionItemRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TrackerUser;
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
		TrackerUser owner = new TrackerUser("owner", "owner@example.com", "encoded", new Privilege(PrivilegeRole.Owner));
		business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		lenient().when(userService.businessForActor("worker")).thenReturn(business);
		lenient().when(userService.businessForActor("owner")).thenReturn(business);
		transactionService = transactionServiceAt(LocalDateTime.of(2026, 6, 1, 12, 0));
	}

	@Test
	void createTransactionRecordsSellItemsAndAppliesStockMovementByUnitCode() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product(9L, "Beer", ProductCategory.Alcohol, false);
		List<ProductBarcode> units = productUnits(product, "UNIT-1", "UNIT-2", "UNIT-3");
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByUnitCodeIn(List.of("UNIT-1", "UNIT-2", "UNIT-3"))).thenReturn(units);
		when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 3, new BigDecimal("18.50"), List.of("UNIT-1", "UNIT-2", "UNIT-3")))),
				"worker");

		assertThat(result.getType()).isEqualTo(TransactionType.SELL);
		assertThat(result.getPaymentType()).isEqualTo(TransactionPaymentType.CASH);
		assertThat(result.getPaymentStatus()).isEqualTo(TransactionPaymentStatus.NOT_REQUIRED);
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().getBarcodes()).containsExactly("UNIT-1", "UNIT-2", "UNIT-3");
		assertThat(result.getTotalAmount()).isEqualByComparingTo("55.50");
		verify(productService).applyStockMovement(product, TransactionType.SELL, 3);
		verify(appEmailService).sendTransactionCreatedOwnerEmail(result);
		verify(stripeService, never()).createTransactionPaymentLink(any(InventoryTransaction.class));
	}

	@Test
	void createTransactionCreatesWebsitePaymentLinkAndEmailsCustomer() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product beer = product(9L, "Beer", ProductCategory.Alcohol, false);
		Product water = product(12L, "Water", ProductCategory.NonAlcohol, false);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(beer);
		when(productService.getProductForStockUpdate(12L, 1L)).thenReturn(water);
		when(productBarcodeRepository.findByUnitCodeIn(List.of("BEER-1", "BEER-2"))).thenReturn(productUnits(beer, "BEER-1", "BEER-2"));
		when(productBarcodeRepository.findByUnitCodeIn(List.of("WATER-1"))).thenReturn(productUnits(water, "WATER-1"));
		when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
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
						new TransactionItemRequest(9L, 2, new BigDecimal("20.50"), List.of("BEER-1", "BEER-2")),
						new TransactionItemRequest(12L, 1, new BigDecimal("12.00"), List.of("WATER-1")))),
				"worker");

		assertThat(result.getPaymentType()).isEqualTo(TransactionPaymentType.WEBSITE_PAYMENT);
		assertThat(result.getPaymentStatus()).isEqualTo(TransactionPaymentStatus.PENDING);
		assertThat(result.getPaymentEmail()).isEqualTo("customer@example.com");
		assertThat(result.getPaymentReference()).isEqualTo("plink_123");
		assertThat(result.getPaymentUrl()).isEqualTo("https://pay.stripe.com/plink_123");
		assertThat(result.getItems()).hasSize(2);
		verify(stripeService).createTransactionPaymentLink(result);
		verify(appEmailService).sendTransactionWebsitePaymentEmail(result);
	}

	@Test
	void createCashTransactionUsesReturnableReferenceEmailWithoutCustomerPaymentEmail() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product(9L, "Beer", ProductCategory.Alcohol, true);
		List<ProductBarcode> units = productUnits(product, "UNIT-1");
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByUnitCodeIn(List.of("UNIT-1"))).thenReturn(units);
		when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryTransaction result = transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("UNIT-1"), "Customer@Example.com"))),
				"worker");

		assertThat(result.getPaymentType()).isEqualTo(TransactionPaymentType.CASH);
		assertThat(result.getPaymentEmail()).isNull();
		assertThat(units.getFirst().getReferenceEmail()).isEqualTo("customer@example.com");
	}

	@Test
	void createTransactionRejectsSellPayloadWithoutScannedUnitCodes() {
		when(userService.getUserById(2L)).thenReturn(user("worker", PrivilegeRole.Worker));
		when(userService.getUserById(1L)).thenReturn(user("owner", PrivilegeRole.Owner));
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product(9L, "Beer", ProductCategory.Alcohol, false));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 2, null))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("SELL transaction requires scanned stock unit codes");
	}

	@Test
	void createTransactionRejectsSellUnitCodeCountMismatch() {
		when(userService.getUserById(2L)).thenReturn(user("worker", PrivilegeRole.Worker));
		when(userService.getUserById(1L)).thenReturn(user("owner", PrivilegeRole.Owner));
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product(9L, "Beer", ProductCategory.Alcohol, false));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 2, null, List.of("UNIT-1")))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("SELL transaction stock unit count must match quantity");
	}

	@Test
	void createTransactionRejectsUnitCodeFromDifferentProduct() {
		Product product = product(9L, "Beer", ProductCategory.Alcohol, false);
		Product otherProduct = product(12L, "Water", ProductCategory.NonAlcohol, false);
		when(userService.getUserById(2L)).thenReturn(user("worker", PrivilegeRole.Worker));
		when(userService.getUserById(1L)).thenReturn(user("owner", PrivilegeRole.Owner));
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(productBarcodeRepository.findByUnitCodeIn(List.of("OTHER-1"))).thenReturn(productUnits(otherProduct, "OTHER-1"));

		assertThatThrownBy(() -> transactionService.createTransaction(new CreateTransactionRequest(
				TransactionType.SELL,
				TransactionPaymentType.CASH,
				null,
				2L,
				1L,
				List.of(new TransactionItemRequest(9L, 1, null, List.of("OTHER-1")))),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Every SELL stock unit code must be registered to the selected product");
	}

	@Test
	void createBuyTransactionUsesProductPriceWhenUnitPriceIsMissing() {
		TrackerUser employee = user("worker", PrivilegeRole.Worker);
		TrackerUser owner = user("owner", PrivilegeRole.Owner);
		Product product = product(9L, "Beer", ProductCategory.Alcohol, false);
		when(userService.getUserById(2L)).thenReturn(employee);
		when(userService.getUserById(1L)).thenReturn(owner);
		when(productService.getProductForStockUpdate(9L, 1L)).thenReturn(product);
		when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
	void getTransactionByIdThrowsWhenMissing() {
		when(transactionRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.getTransactionById(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Transaction not found: 7");
	}

	private Product product(Long id, String name, ProductCategory category, boolean returnable) {
		Product product = new Product(name, category, new BigDecimal("20.00"), 10, returnable);
		product.setProductBarcode("GTIN-" + id);
		ReflectionTestUtils.setField(product, "id", id);
		return product;
	}

	private List<ProductBarcode> productUnits(Product product, String... unitCodes) {
		return java.util.Arrays.stream(unitCodes)
				.map(unitCode -> {
					ProductBarcode productBarcode = new ProductBarcode(unitCode, product.getProductBarcode());
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
