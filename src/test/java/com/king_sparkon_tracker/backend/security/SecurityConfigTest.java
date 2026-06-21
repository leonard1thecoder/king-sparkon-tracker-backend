package com.king_sparkon_tracker.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.king_sparkon_tracker.backend.config.CorsConfig;
import com.king_sparkon_tracker.backend.config.JacksonConfig;
import com.king_sparkon_tracker.backend.controller.AuthenticationController;
import com.king_sparkon_tracker.backend.controller.UserController;
import com.king_sparkon_tracker.backend.controller.HealthController;
import com.king_sparkon_tracker.backend.controller.ProductBarcodeController;
import com.king_sparkon_tracker.backend.controller.ProductController;
import com.king_sparkon_tracker.backend.controller.ReportController;
import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.AlcoholReportResponse;
import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductBarcodeService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductService;
import com.king_sparkon_tracker.backend.service.ReportService;

@WebMvcTest(controllers = {
		AuthenticationController.class,
		UserController.class,
		HealthController.class,
		ProductBarcodeController.class,
		ProductController.class,
		ReportController.class
})
@Import({ SecurityConfig.class, CorsConfig.class, JacksonConfig.class, ApiExceptionHandler.class })
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TrackerUserService userService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private ReportService reportService;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private ProductBarcodeService productBarcodeService;

	@MockitoBean
	private ProductPricingService productPricingService;

	@MockitoBean
	private PriceLocalizationService priceLocalizationService;

	@MockitoBean
	private PasswordResetService passwordResetService;

	@MockitoBean
	private EmailVerificationService emailVerificationService;

	@MockitoBean
	private BusinessAccessService businessAccessService;

	@MockitoBean
	private RateLimitService rateLimitService;

	@MockitoBean
	private DataSource dataSource;

	@Test
	void corsPreflightAllowsNextJsLocalOrigin() throws Exception {
		mockMvc.perform(options("/api/products")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void corsPreflightRejectsUnconfiguredOrigin() throws Exception {
		mockMvc.perform(options("/api/products")
				.header(HttpHeaders.ORIGIN, "https://unknown.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
	}

	@Test
	void healthCheckIsPublic() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("backend"));
	}

	@Test
	void ownerRegistrationIsPublic() throws Exception {
		when(userService.registerOwner(any(RegisterUserRequest.class)))
				.thenReturn(trackerUser("owner", "owner@kingsparkon.co.za", PrivilegeRole.Owner));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "owner",
						  "emailAddress": "owner@kingsparkon.co.za",
						  "password": "secret",
						  "businessName": "Owner Store"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.privilege").value("Owner"));
	}

	@Test
	void workerCreationRejectsUnauthenticatedCaller() throws Exception {
		mockMvc.perform(post("/api/users/workers")
				.contentType(MediaType.APPLICATION_JSON)
				.content(workerJson()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void workerCreationRejectsWorkerCaller() throws Exception {
		mockMvc.perform(post("/api/users/workers")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(workerJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void workerCreationAllowsOwnerCaller() throws Exception {
		when(userService.createWorker(any(CreateWorkerRequest.class), eq("owner")))
				.thenReturn(trackerUser("worker", "worker@example.com", PrivilegeRole.Worker));

		mockMvc.perform(post("/api/users/workers")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(workerJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("worker"))
				.andExpect(jsonPath("$.privilege").value("Worker"));
	}

	@Test
	void productCreationRejectsWorkerCaller() throws Exception {
		mockMvc.perform(post("/api/products")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void productCreationAllowsOwnerCaller() throws Exception {
		when(productService.createProduct(any(ProductRequest.class), eq("owner")))
				.thenReturn(product("Water", ProductCategory.NonAlcohol, 20));

		mockMvc.perform(post("/api/products")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(productJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Water"))
				.andExpect(jsonPath("$.remainingBarcodeSlots").value(20));
	}

	@Test
	void barcodeAssignmentRejectsOwnerCaller() throws Exception {
		mockMvc.perform(post("/api/products/7/barcodes")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(barcodeJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void barcodeAssignmentAllowsWorkerCaller() throws Exception {
		when(productService.addBarcodeToProduct(eq(7L), any(AddProductBarcodeRequest.class), eq("worker")))
				.thenReturn(product("Water", "6001", ProductCategory.NonAlcohol, 20));

		mockMvc.perform(post("/api/products/7/barcodes")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(barcodeJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.barcodes[0].barcode").value("6001"));
	}

	@Test
	void quantityUpdateRejectsWorkerCaller() throws Exception {
		mockMvc.perform(patch("/api/products/7/quantity")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(quantityJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void quantityUpdateAllowsOwnerCaller() throws Exception {
		when(productService.updateProductQuantity(eq(7L), any(UpdateProductQuantityRequest.class), eq("owner")))
				.thenReturn(product("Water", ProductCategory.NonAlcohol, 20));

		mockMvc.perform(patch("/api/products/7/quantity")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(quantityJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockQuantity").value(20));
	}

	@Test
	void approvalSubmissionRejectsOwnerCaller() throws Exception {
		mockMvc.perform(post("/api/products/7/submit-approval")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name())))
				.andExpect(status().isForbidden());
	}

	@Test
	void approvalSubmissionAllowsWorkerCaller() throws Exception {
		when(productService.submitProductForApproval(7L, "worker"))
				.thenReturn(product("Water", ProductCategory.NonAlcohol, 20));

		mockMvc.perform(post("/api/products/7/submit-approval")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Water"));
	}

	@Test
	void barcodeReferenceSearchRejectsAnonymousCaller() throws Exception {
		mockMvc.perform(get("/api/barcodes/reference/0821234567"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void barcodeReferenceSearchAllowsAuthenticatedWorker() throws Exception {
		ProductBarcode barcode = product("Water", "6001", ProductCategory.NonAlcohol, 20, true).getBarcodes().getFirst();
		barcode.setReferencee("0821234567");
		when(productBarcodeService.findByReference("0821234567", "worker"))
				.thenReturn(java.util.List.of(barcode));

		mockMvc.perform(get("/api/barcodes/reference/0821234567")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].barcode").value("6001"));
	}

	@Test
	void barcodeClaimAllowsAuthenticatedWorker() throws Exception {
		ProductBarcode barcode = product("Water", "6001", ProductCategory.NonAlcohol, 20, true).getBarcodes().getFirst();
		barcode.setReferencee("0821234567");
		barcode.setStatus(ProductBarcodeStatus.CLAIMED);
		when(productBarcodeService.claimByReference("0821234567", "worker"))
				.thenReturn(barcode);

		mockMvc.perform(post("/api/barcodes/reference/0821234567/claim")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLAIMED"));
	}

	@Test
	void alcoholReportRejectsWorkerCaller() throws Exception {
		mockMvc.perform(get("/api/reports/alcohol")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name())))
				.andExpect(status().isForbidden());
	}

	@Test
	void alcoholReportAllowsOwnerCaller() throws Exception {
		when(reportService.alcoholReport(isNull(), isNull(), eq("owner"))).thenReturn(new AlcoholReportResponse(
				null,
				null,
				10,
				5,
				new BigDecimal("150.00"),
				new BigDecimal("102.00")));

		mockMvc.perform(get("/api/reports/alcohol")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.boughtQuantity").value(10))
				.andExpect(jsonPath("$.soldQuantity").value(5));
	}

	private String workerJson() {
		return """
				{
				  "username": "worker",
				  "emailAddress": "worker@example.com",
				  "password": "secret"
				}
				""";
	}

	private String productJson() {
		return """
				{
				  "name": "Water",
				  "category": "NonAlcohol",
				  "price": 10.00,
				  "returnableEnabled": false,
				  "returnablePrice": null,
				  "nightShiftEnabled": false,
				  "nightShiftPrice": null,
				  "nightShiftStartTime": null,
				  "nightShiftEndTime": null,
				  "stockQuantity": 20
				}
				""";
	}

	private String barcodeJson() {
		return """
				{
				  "barcode": "6001"
				}
				""";
	}

	private String quantityJson() {
		return """
				{
				  "stockQuantity": 20
				}
				""";
	}

	private TrackerUser trackerUser(String username, String emailAddress, PrivilegeRole role) {
		return new TrackerUser(username, emailAddress, "encoded", new Privilege(role));
	}

	private Product product(String name, ProductCategory category, int stockQuantity) {
		return new Product(name, category, BigDecimal.TEN, stockQuantity);
	}

	private Product product(String name, String barcode, ProductCategory category, int stockQuantity) {
		return new Product(name, barcode, category, BigDecimal.TEN, stockQuantity);
	}

	private Product product(
			String name,
			String barcode,
			ProductCategory category,
			int stockQuantity,
			boolean bottleReturnable) {
		return new Product(name, barcode, category, BigDecimal.TEN, stockQuantity, bottleReturnable);
	}
}
