package com.king_sparkon_tracker.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.service.ProductBarcodeService;

class ProductBarcodeControllerTest {

	private ProductBarcodeService productBarcodeService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		productBarcodeService = mock(ProductBarcodeService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ProductBarcodeController(productBarcodeService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void findByReferenceReturnsBarcodeRows() throws Exception {
		when(productBarcodeService.findByReference("0821234567", "worker"))
				.thenReturn(List.of(barcode(ProductBarcodeStatus.NOT_CLAIMED)));

		mockMvc.perform(get("/api/barcodes/reference/0821234567")
				.principal(() -> "worker"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].barcode").value("6001"))
				.andExpect(jsonPath("$[0].referencee").value("0821234567"))
				.andExpect(jsonPath("$[0].status").value("NOT_CLAIMED"))
				.andExpect(jsonPath("$[0].productName").value("Castle Lite"))
				.andExpect(jsonPath("$[0].bottleReturnable").value(true));
	}

	@Test
	void claimByReferenceReturnsClaimedBarcode() throws Exception {
		when(productBarcodeService.claimByReference("0821234567", "worker"))
				.thenReturn(barcode(ProductBarcodeStatus.CLAIMED));

		mockMvc.perform(post("/api/barcodes/reference/0821234567/claim")
				.principal(() -> "worker"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.referencee").value("0821234567"))
				.andExpect(jsonPath("$.status").value("CLAIMED"));
	}

	@Test
	void claimByIdReturnsClaimedBarcode() throws Exception {
		when(productBarcodeService.claimById(14L, "worker"))
				.thenReturn(barcode(ProductBarcodeStatus.CLAIMED));

		mockMvc.perform(post("/api/barcodes/14/claim")
				.principal(() -> "worker"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLAIMED"));
	}

	@Test
	void findByReferenceMapsMissingReferenceToNotFound() throws Exception {
		when(productBarcodeService.findByReference("missing", "worker"))
				.thenThrow(new ResourceNotFoundException("Barcode not found for reference: missing"));

		mockMvc.perform(get("/api/barcodes/reference/missing")
				.principal(() -> "worker"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Barcode not found for reference: missing"));
	}

	@Test
	void claimByReferenceMapsExpiredClaimToBadRequest() throws Exception {
		when(productBarcodeService.claimByReference("0821234567", "worker"))
				.thenThrow(new IllegalArgumentException("Returnable barcode claim has expired"));

		mockMvc.perform(post("/api/barcodes/reference/0821234567/claim")
				.principal(() -> "worker"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Returnable barcode claim has expired"));
	}

	private ProductBarcode barcode(ProductBarcodeStatus status) {
		Product product = new Product(
				"Castle Lite",
				"6001",
				ProductCategory.Alcohol,
				new BigDecimal("20.50"),
				10,
				true);
		ProductBarcode barcode = product.getBarcodes().getFirst();
		barcode.setReferencee("0821234567");
		barcode.setStatus(status);
		return barcode;
	}
}
