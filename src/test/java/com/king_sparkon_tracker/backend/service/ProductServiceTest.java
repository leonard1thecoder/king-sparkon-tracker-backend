package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateBarcodeException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.BarcodeCatalog;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.BarcodeCatalogRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductBarcodeRepository productBarcodeRepository;

	@Mock
	private BarcodeCatalogRepository barcodeCatalogRepository;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private TrackerUserService userService;

	@Mock
	private BusinessAccessService businessAccessService;

	@Mock
	private AppEmailService appEmailService;

	private ProductService productService;

	@BeforeEach
	void setUp() {
		productService = new ProductService(
				productRepository,
				productBarcodeRepository,
				barcodeCatalogRepository,
				auditLogService,
				userService,
				businessAccessService,
				appEmailService);
		lenient().when(barcodeCatalogRepository.findByBarcode(any())).thenReturn(Optional.empty());
		lenient().when(barcodeCatalogRepository.save(any(BarcodeCatalog.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createProductSavesProduct() {
		Business business = business();
		when(userService.businessForActor("owner")).thenReturn(business);
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Product result = productService.createProduct(
				new ProductRequest(
						" Castle Lite ",
						"600123",
						ProductCategory.Alcohol,
						new BigDecimal("20.50"),
						true,
						new BigDecimal("3.00"),
						false,
						null,
						null,
						null,
						10,
						null),
				"owner");

		assertThat(result.getName()).isEqualTo("Castle Lite");
		assertThat(result.getProductBarcode()).isEqualTo("600123");
		assertThat(result.getBusiness()).isSameAs(business);
		assertThat(result.getBarcodes()).isEmpty();
		assertThat(result.getStatus()).isEqualTo(ProductStatus.CREATED);
		assertThat(result.getPrice()).isEqualByComparingTo("20.50");
		assertThat(result.isBottleReturnable()).isTrue();
		assertThat(result.getStockQuantity()).isEqualTo(10);
	}

	@Test
	void addBarcodeToProductCreatesStockUnitForReturnableProduct() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10, true);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productRepository.findWithBarcodesByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		Product result = productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("UNIT-1", " 12345 ", " customer@example.com "),
				"worker");

		assertThat(result.getProductBarcode()).isEqualTo("12345");
		assertThat(result.getBarcodes()).hasSize(1);
		assertThat(result.getBarcodes().getFirst().getUnitCode()).isEqualTo("UNIT-1");
		assertThat(result.getBarcodes().getFirst().getBarcode()).isEqualTo("12345");
		assertThat(result.getBarcodes().getFirst().getReferenceEmail()).isEqualTo("customer@example.com");
		assertThat(result.getBarcodes().getFirst().getStatus()).isEqualTo(ProductBarcodeStatus.NOT_CLAIMED);
		verify(productBarcodeRepository).save(any(ProductBarcode.class));
	}

	@Test
	void addBarcodeToProductRejectsDuplicateProductBarcodeWhenAssigningFirstStockUnit() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10, false);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productRepository.existsByBusiness_IdAndProductBarcode(business.getId(), "12345")).thenReturn(true);

		assertThatThrownBy(() -> productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("UNIT-1", "12345", null),
				"worker"))
				.isInstanceOf(DuplicateBarcodeException.class)
				.hasMessage("Barcode already exists: 12345");
	}

	@Test
	void addBarcodeToProductRejectsProductsAlreadySubmittedForApproval() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 10);
		product.setProductBarcode("12345");
		product.setStatus(ProductStatus.PENDING_APPROVAL);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));

		assertThatThrownBy(() -> productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("UNIT-1", "12345", null),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Stock units can only be added to CREATED products");
	}

	@Test
	void addBarcodeToProductRejectsWhenStockUnitCapacityIsReached() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 2);
		product.setProductBarcode("12345");
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(2L);

		assertThatThrownBy(() -> productService.addBarcodeToProduct(
				7L,
				new AddProductBarcodeRequest("UNIT-1", "12345", null),
				"worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Product already has stock units for all stock quantity");
	}

	@Test
	void updateProductQuantityRejectsQuantityBelowStockUnitCount() {
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
				.hasMessage("Stock quantity cannot be lower than assigned stock unit count");
	}

	@Test
	void submitProductForApprovalRequiresStockUnitCountToMatchStockQuantity() {
		Business business = business();
		Product product = product("Water", ProductCategory.NonAlcohol, 2);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productRepository.findLockedByIdAndBusiness_Id(7L, business.getId())).thenReturn(Optional.of(product));
		when(productBarcodeRepository.countByProduct_Id(7L)).thenReturn(1L);

		assertThatThrownBy(() -> productService.submitProductForApproval(7L, "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Product stock unit count must match stock quantity before approval submission");
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
		verify(auditLogService).record(
				eq("PRODUCT_SUBMITTED_FOR_APPROVAL"),
				eq("Product"),
				any(),
				eq("worker"),
				eq("Product submitted for approval with stock unit count: 2"),
				eq(business));
	}

	@Test
	void listTuckShopProductsWithSearchUsesNormalizedSearchPredicate() {
		PageRequest pageable = PageRequest.of(0, 20);
		when(productRepository.searchTuckShopProducts(ProductStatus.CREATED, null, ProductCategory.NonAlcohol, "water", pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		productService.listTuckShopProducts(pageable, null, ProductCategory.NonAlcohol, " water ");

		verify(productRepository).searchTuckShopProducts(ProductStatus.CREATED, null, ProductCategory.NonAlcohol, "water", pageable);
	}

	@Test
	void getProductByBarcodeReturnsScannedProductBarcode() {
		Product product = product("Water", "6001", ProductCategory.NonAlcohol, 10);
		when(productRepository.findFirstByProductBarcode("6001")).thenReturn(Optional.of(product));

		assertThat(productService.getProductByBarcode(" 6001 ")).isSameAs(product);
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
	void getProductForStockUpdateThrowsWhenMissing() {
		when(productRepository.findLockedById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProductForStockUpdate(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product not found: 7");
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
