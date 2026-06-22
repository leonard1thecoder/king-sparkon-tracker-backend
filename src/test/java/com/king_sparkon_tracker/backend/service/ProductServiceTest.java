package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateBarcodeException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductBarcodeRepository productBarcodeRepository;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private TrackerUserService userService;

	@Mock
	private BusinessAccessService businessAccessService;

	@Mock
	private AppEmailService appEmailService;

	@InjectMocks
	private ProductService productService;

	@Test
	void createProductSavesProduct() {
		Business business = business();
		when(userService.businessForActor("owner")).thenReturn(business);
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Product result = productService.createProduct(
				new ProductRequest(
						" Castle Lite ",
						ProductCategory.Alcohol,
						new BigDecimal("20.50"),
						true,
						new BigDecimal("3.00"),
						false,
						null,
						null,
						null,
						10),
				"owner");

		assertThat(result.getName()).isEqualTo("Castle Lite");
		assertThat(result.getBusiness()).isSameAs(business);
		assertThat(result.getBarcodes()).isEmpty();
		assertThat(result.getCategory()).isEqualTo(ProductCategory.Alcohol);
		assertThat(result.getStatus()).isEqualTo(ProductStatus.CREATED);
		assertThat(result.getPrice()).isEqualByComparingTo("20.50");
		assertThat(result.isBottleReturnable()).isTrue();
		assertThat(result.getStockQuantity()).isEqualTo(10);
	}

	@Test
	void addBarcodeToProductSavesReturnableBarcodeAsNotClaimedWithReference() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10, true);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(1L);
		when(productBarcodeRepository.existsByBarcode("12345")).thenReturn(false);
		when(productRepository.findWithBarcodesByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		Product result = productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest(" 12345 ", " 0821234567 "),
				"worker");

		assertThat(result.getBarcodes()).extracting("barcode").containsExactly("12345");
		assertThat(result.getBarcodes().getFirst().getReferencee()).isEqualTo("0821234567");
		assertThat(result.getBarcodes().getFirst().getStatus()).isEqualTo(ProductBarcodeStatus.NOT_CLAIMED);
		verify(productBarcodeRepository).save(any());
	}

	@Test
	void addBarcodeToProductSavesNonReturnableBarcodeAsNotClaimable() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(1L);
		when(productBarcodeRepository.existsByBarcode("12345")).thenReturn(false);
		when(productRepository.findWithBarcodesByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		Product result = productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest(" 12345 ", null),
				"worker");

		assertThat(result.getBarcodes().getFirst().getStatus()).isEqualTo(ProductBarcodeStatus.NOT_CLAIMABLE);
	}

	@Test
	void addBarcodeToProductRejectsProductsAlreadySubmittedForApproval() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10);
		product.setStatus(ProductStatus.PENDING_APPROVAL);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("12345", null),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Barcodes can only be added to CREATED products");
	}

	@Test
	void addBarcodeToProductRejectsDuplicateBarcode() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(0L);
		when(productBarcodeRepository.existsByBarcode("12345")).thenReturn(true);

		assertThatThrownBy(() -> productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("12345", null),
				"worker"))
				.isInstanceOf(DuplicateBarcodeException.class)
				.hasMessage("Barcode already exists: 12345");
	}

	@Test
	void addBarcodeToProductRejectsWhenStockBarcodeCapacityIsReached() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 2);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(2L);

		assertThatThrownBy(() -> productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("12345", null),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Product already has barcodes for all stock units");
	}

	@Test
	void createProductRejectsNegativeStock() {
		assertThatThrownBy(() -> productService.createProduct(
				new ProductRequest(
						"Water",
						ProductCategory.NonAlcohol,
						BigDecimal.TEN,
						false,
						null,
						false,
						null,
						null,
						null,
						-1),
				"owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Stock quantity cannot be negative");
	}

	@Test
	void updateProductQuantitySavesQuantityAndResetsStatusToCreated() {
		Business business = business();
		Product product = product("Water", "1", ProductCategory.NonAlcohol, 10);
		product.setStatus(ProductStatus.PENDING_APPROVAL);
		when(userService.businessForActor("owner")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(1L);
		when(productRepository.save(product)).thenReturn(product);
		when(productRepository.findWithBarcodesByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		Product result = productService.updateProductQuantity(
				7L,
				new UpdateProductQuantityRequest(12),
				"owner");

		assertThat(result.getStockQuantity()).isEqualTo(12);
		assertThat(result.getStatus()).isEqualTo(ProductStatus.CREATED);
		verify(productRepository).save(product);
	}

	@Test
	void updateProductQuantityRejectsQuantityBelowBarcodeCount() {
		Business business = business();
		Product product = product("Water", "1", ProductCategory.NonAlcohol, 10);
		when(userService.businessForActor("owner")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(1L);

		assertThatThrownBy(() -> productService.updateProductQuantity(
				7L,
				new UpdateProductQuantityRequest(0),
				"owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Stock quantity cannot be lower than assigned barcode count");
	}

	@Test
	void submitProductForApprovalRequiresBarcodeCountToMatchStockQuantity() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 2);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(1L);

		assertThatThrownBy(() -> productService.submitProductForApproval(7L, "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Product barcode count must match stock quantity before approval submission");
	}

	@Test
	void submitProductForApprovalMovesProductToPendingApproval() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 2);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(2L);
		when(productRepository.save(product)).thenReturn(product);
		when(productRepository.findWithBarcodesByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		Product result = productService.submitProductForApproval(7L, "worker");

		assertThat(result.getStatus()).isEqualTo(ProductStatus.PENDING_APPROVAL);
		verify(productRepository).save(product);
		verify(appEmailService).sendProductApprovalRequestEmail(business, product, "worker", 2L);
	}

	@Test
	void submitProductForApprovalContinuesWhenApprovalEmailFails() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 2);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(2L);
		when(productRepository.save(product)).thenReturn(product);
		when(productRepository.findWithBarcodesByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(appEmailService.sendProductApprovalRequestEmail(business, product, "worker", 2L))
				.thenThrow(new IllegalStateException("smtp down"));

		Product result = productService.submitProductForApproval(7L, "worker");

		assertThat(result.getStatus()).isEqualTo(ProductStatus.PENDING_APPROVAL);
		verify(auditLogService).record(
				eq("PRODUCT_SUBMITTED_FOR_APPROVAL"),
				eq("Product"),
				any(),
				eq("worker"),
				eq("Product submitted for approval with barcode count: 2"),
				eq(business));
	}

	@Test
	void listProductsReturnsAllProducts() {
		List<Product> products = List.of(product("Water", "1", ProductCategory.NonAlcohol, 10));
		when(productRepository.findAll()).thenReturn(products);

		assertThat(productService.listProducts()).containsExactlyElementsOf(products);
	}

	@Test
	void getProductByIdReturnsProduct() {
		Product product = product("Water", "1", ProductCategory.NonAlcohol, 10);
		when(productRepository.findWithBarcodesById(7L)).thenReturn(Optional.of(product));

		assertThat(productService.getProductById(7L)).isSameAs(product);
	}

	@Test
	void getProductByIdThrowsWhenMissing() {
		when(productRepository.findWithBarcodesById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProductById(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found: 7");
	}

	@Test
	void getProductForStockUpdateReturnsLockedProduct() {
		Product product = product("Water", "1", ProductCategory.NonAlcohol, 10);
		when(productRepository.findLockedById(7L)).thenReturn(Optional.of(product));

		assertThat(productService.getProductForStockUpdate(7L)).isSameAs(product);
	}

	@Test
	void getProductForStockUpdateThrowsWhenMissing() {
		when(productRepository.findLockedById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProductForStockUpdate(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found: 7");
	}

	@Test
	void getProductByBarcodeReturnsScannedProduct() {
		Product product = product("Water", "6001", ProductCategory.NonAlcohol, 10);
		when(productBarcodeRepository.findByBarcode("6001")).thenReturn(Optional.of(product.getBarcodes().getFirst()));

		assertThat(productService.getProductByBarcode(" 6001 ")).isSameAs(product);
	}

	@Test
	void applyStockMovementIncreasesStockOnBuy() {
		Product product = product("Water", "6001", ProductCategory.NonAlcohol, 10);
		when(productRepository.save(product)).thenReturn(product);

		Product result = productService.applyStockMovement(product, TransactionType.BUY, 5);

		assertThat(result.getStockQuantity()).isEqualTo(15);
		verify(productRepository).save(product);
	}

	@Test
	void applyStockMovementDecreasesStockOnSell() {
		Product product = product("Water", "6001", ProductCategory.NonAlcohol, 10);
		when(productRepository.save(product)).thenReturn(product);

		Product result = productService.applyStockMovement(product, TransactionType.SELL, 4);

		assertThat(result.getStockQuantity()).isEqualTo(6);
		verify(productRepository).save(product);
	}

	@Test
	void applyStockMovementRejectsInsufficientStockOnSell() {
		Product product = product("Water", "6001", ProductCategory.NonAlcohol, 3);

		assertThatThrownBy(() -> productService.applyStockMovement(product, TransactionType.SELL, 4))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Not enough stock for product: Water");
	}

	private Product product(String name, ProductCategory category, int stockQuantity) {
		return new Product(name, category, new BigDecimal("10.00"), stockQuantity);
	}

	private Product product(String name, ProductCategory category, int stockQuantity, boolean bottleReturnable) {
		return new Product(name, category, new BigDecimal("10.00"), stockQuantity, bottleReturnable);
	}

	private Product product(String name, String barcode, ProductCategory category, int stockQuantity) {
		return new Product(name, barcode, category, new BigDecimal("10.00"), stockQuantity);
	}

	private Business business() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		return business;
	}
}
