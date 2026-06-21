package com.king_sparkon_tracker.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.exception.DuplicateBarcodeException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductService;

class ProductControllerTest {

	private ProductService productService;
	private ProductPricingService productPricingService;
	private PriceLocalizationService priceLocalizationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		productService = mock(ProductService.class);
		productPricingService = mock(ProductPricingService.class);
		priceLocalizationService = mock(PriceLocalizationService.class);
		when(productPricingService.priceForSale(any(Product.class))).thenReturn(new BigDecimal("20.50"));
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ProductController(productService, productPricingService, priceLocalizationService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void createProductReturnsCreatedProduct() throws Exception {
		when(productService.createProduct(any(ProductRequest.class), eq("owner")))
				.thenReturn(product("Beer", ProductCategory.Alcohol, 10, true));

		mockMvc.perform(post("/api/products")
				.principal(() -> "owner")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Beer",
						  "category": "Alcohol",
						  "price": 20.50,
						  "returnableEnabled": true,
						  "returnablePrice": 3.00,
						  "nightShiftEnabled": false,
						  "nightShiftPrice": null,
						  "nightShiftStartTime": null,
						  "nightShiftEndTime": null,
						  "stockQuantity": 10
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Beer"))
				.andExpect(jsonPath("$.status").value("CREATED"))
				.andExpect(jsonPath("$.salePrice").value(20.50))
				.andExpect(jsonPath("$.bottleReturnable").value(true))
				.andExpect(jsonPath("$.barcodes").isEmpty())
				.andExpect(jsonPath("$.remainingBarcodeSlots").value(10))
				.andExpect(jsonPath("$.category").value("Alcohol"))
				.andExpect(jsonPath("$.stockQuantity").value(10));
	}

	@Test
	void addBarcodeToProductReturnsUpdatedProduct() throws Exception {
		Product product = product("Beer", "6001", ProductCategory.Alcohol, 10, true);
		product.getBarcodes().getFirst().setReferencee("0821234567");
		when(productService.addBarcodeToProduct(eq(7L), any(AddProductBarcodeRequest.class), eq("worker")))
				.thenReturn(product);

		mockMvc.perform(post("/api/products/7/barcodes")
				.principal(() -> "worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "barcode": "6001",
						  "referencee": "0821234567"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Beer"))
				.andExpect(jsonPath("$.barcodes[0].barcode").value("6001"))
				.andExpect(jsonPath("$.barcodes[0].referencee").value("0821234567"))
				.andExpect(jsonPath("$.barcodes[0].status").value("NOT_CLAIMED"))
				.andExpect(jsonPath("$.barcodeCount").value(1))
				.andExpect(jsonPath("$.remainingBarcodeSlots").value(9));
	}

	@Test
	void updateProductQuantityReturnsUpdatedProduct() throws Exception {
		when(productService.updateProductQuantity(eq(7L), any(UpdateProductQuantityRequest.class), eq("owner")))
				.thenReturn(product("Beer", ProductCategory.Alcohol, 12));

		mockMvc.perform(patch("/api/products/7/quantity")
				.principal(() -> "owner")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "stockQuantity": 12
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockQuantity").value(12))
				.andExpect(jsonPath("$.status").value("CREATED"));
	}

	@Test
	void submitProductForApprovalReturnsPendingApprovalProduct() throws Exception {
		Product product = product("Beer", ProductCategory.Alcohol, 10);
		product.setStatus(ProductStatus.PENDING_APPROVAL);
		when(productService.submitProductForApproval(7L, "worker")).thenReturn(product);

		mockMvc.perform(post("/api/products/7/submit-approval")
				.principal(() -> "worker"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
	}

	@Test
	void addBarcodeToProductMapsDuplicateBarcodeToConflict() throws Exception {
		when(productService.addBarcodeToProduct(eq(7L), any(AddProductBarcodeRequest.class), eq("worker")))
				.thenThrow(new DuplicateBarcodeException("6001"));

		mockMvc.perform(post("/api/products/7/barcodes")
				.principal(() -> "worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "barcode": "6001"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Barcode already exists: 6001"));
	}

	@Test
	void listProductsReturnsProducts() throws Exception {
		when(productService.listProducts(PageRequest.of(0, 20), "owner")).thenReturn(new PageImpl<>(
				java.util.List.of(
						product("Beer", "6001", ProductCategory.Alcohol, 10),
						product("Water", "6002", ProductCategory.NonAlcohol, 12)),
				PageRequest.of(0, 20),
				2));

		mockMvc.perform(get("/api/products").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Beer"))
				.andExpect(jsonPath("$.content[1].category").value("NonAlcohol"))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void getProductByIdReturnsProduct() throws Exception {
		when(productService.getProductById(7L, "owner")).thenReturn(product("Water", "6002", ProductCategory.NonAlcohol, 12));

		mockMvc.perform(get("/api/products/7").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.barcodes[0].barcode").value("6002"))
				.andExpect(jsonPath("$.barcodes[0].status").value("NOT_CLAIMABLE"));
	}

	@Test
	void getProductByBarcodeReturnsScannedProduct() throws Exception {
		Product product = product("Water", "6002", ProductCategory.NonAlcohol, 12, true);
		when(productService.getProductByBarcode("6002", "worker"))
				.thenReturn(product);
		when(productPricingService.priceForSale(product)).thenReturn(new BigDecimal("20.50"));

		mockMvc.perform(get("/api/products/barcode/6002").principal(() -> "worker"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Water"))
				.andExpect(jsonPath("$.salePrice").value(20.50))
				.andExpect(jsonPath("$.bottleReturnable").value(true))
				.andExpect(jsonPath("$.barcodes[0].barcode").value("6002"))
				.andExpect(jsonPath("$.barcodes[0].status").value("NOT_CLAIMED"));
	}

	@Test
	void getProductByBarcodeMapsMissingProductToNotFound() throws Exception {
		when(productService.getProductByBarcode("missing", "worker"))
				.thenThrow(new ResourceNotFoundException("Product not found for barcode: missing"));

		mockMvc.perform(get("/api/products/barcode/missing").principal(() -> "worker"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Product not found for barcode: missing"));
	}

	private Product product(String name, String barcode, ProductCategory category, int stockQuantity) {
		return new Product(name, barcode, category, new BigDecimal("20.50"), stockQuantity);
	}

	private Product product(
			String name,
			String barcode,
			ProductCategory category,
			int stockQuantity,
			boolean bottleReturnable) {
		return new Product(name, barcode, category, new BigDecimal("20.50"), stockQuantity, bottleReturnable);
	}

	private Product product(String name, ProductCategory category, int stockQuantity) {
		return new Product(name, category, new BigDecimal("20.50"), stockQuantity);
	}

	private Product product(String name, ProductCategory category, int stockQuantity, boolean bottleReturnable) {
		return new Product(name, category, new BigDecimal("20.50"), stockQuantity, bottleReturnable);
	}
}
