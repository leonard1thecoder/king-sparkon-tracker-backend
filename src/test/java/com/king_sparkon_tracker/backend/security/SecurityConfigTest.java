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
import com.king_sparkon_tracker.backend.controller.AffiliateController;
import com.king_sparkon_tracker.backend.controller.UserController;
import com.king_sparkon_tracker.backend.controller.HealthController;
import com.king_sparkon_tracker.backend.controller.ProductBarcodeController;
import com.king_sparkon_tracker.backend.controller.ProductController;
import com.king_sparkon_tracker.backend.controller.ReportController;
import com.king_sparkon_tracker.backend.controller.TipController;
import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.AffiliateProfileResponse;
import com.king_sparkon_tracker.backend.dto.AlcoholReportResponse;
import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.RegisterAffiliateRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.PayoutAccountStatus;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawalStatus;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;
import com.king_sparkon_tracker.backend.service.AffiliateService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductBarcodeService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductService;
import com.king_sparkon_tracker.backend.service.ReportService;
import com.king_sparkon_tracker.backend.service.TipService;
import com.king_sparkon_tracker.backend.service.TipWithdrawalService;

@WebMvcTest(controllers = {
		AuthenticationController.class,
		AffiliateController.class,
		UserController.class,
		HealthController.class,
		ProductBarcodeController.class,
		ProductController.class,
		ReportController.class,
		TipController.class
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
	private TipService tipService;

	@MockitoBean
	private TipWithdrawalService tipWithdrawalService;

	@MockitoBean
	private AffiliateService affiliateService;

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
	void affiliateRegistrationIsPublic() throws Exception {
		TrackerUser affiliate = trackerUser("affiliate", "affiliate@example.com", PrivilegeRole.Affiliate);
		affiliate.activateAffiliateProfile(
				"AFF-AFFILIATE-1234",
				"https://app.example/pricing?affiliateCode=AFF-AFFILIATE-1234",
				"https://api.qrserver.com/v1/create-qr-code/?data=affiliate");
		when(userService.registerAffiliate(any(RegisterAffiliateRequest.class))).thenReturn(affiliate);

		mockMvc.perform(post("/api/auth/register-affiliate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "affiliate",
						  "emailAddress": "affiliate@example.com",
						  "password": "secret"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.privilege").value("Affiliate"))
				.andExpect(jsonPath("$.affiliateCode").value("AFF-AFFILIATE-1234"));
	}

	@Test
	void affiliateProfileRejectsOwnerCaller() throws Exception {
		mockMvc.perform(get("/api/affiliates/me")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name())))
				.andExpect(status().isForbidden());
	}

	@Test
	void affiliateProfileAllowsAffiliateCaller() throws Exception {
		when(affiliateService.profile("affiliate"))
				.thenReturn(new AffiliateProfileResponse(
						10L,
						"affiliate",
						"affiliate@example.com",
						null,
						null,
						true,
						false,
						null,
						"AFF-AFFILIATE-1234",
						"https://app.example/pricing?affiliateCode=AFF-AFFILIATE-1234",
						"https://api.qrserver.com/v1/create-qr-code/?data=affiliate",
						java.time.LocalDateTime.parse("2026-06-24T10:00:00")));

		mockMvc.perform(get("/api/affiliates/me")
				.with(user("affiliate").authorities(() -> PrivilegeRole.Affiliate.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affiliateCode").value("AFF-AFFILIATE-1234"));
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
	void onboardingCompletionAllowsWorkerCaller() throws Exception {
		TrackerUser worker = trackerUser("worker", "worker@example.com", PrivilegeRole.Worker);
		worker.completeOnboarding("45 Worker Street", "+27821234567");
		when(userService.completeOnboarding(any(), eq("worker"))).thenReturn(worker);

		mockMvc.perform(patch("/api/users/me/onboarding")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "physicalAddress": "45 Worker Street",
						  "cellphoneNumber": "+27821234567"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingCompleted").value(true));
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
		mockMvc.perform(get("/api/barcodes/reference/customer@example.com"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void barcodeReferenceSearchAllowsAuthenticatedWorker() throws Exception {
		ProductBarcode barcode = product("Water", "6001", ProductCategory.NonAlcohol, 20, true).getBarcodes().getFirst();
		barcode.setReferenceEmail("customer@example.com");
		when(productBarcodeService.findByReference("customer@example.com", "worker"))
				.thenReturn(java.util.List.of(barcode));

		mockMvc.perform(get("/api/barcodes/reference/customer@example.com")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].barcode").value("6001"));
	}

	@Test
	void barcodeClaimAllowsAuthenticatedWorker() throws Exception {
		ProductBarcode barcode = product("Water", "6001", ProductCategory.NonAlcohol, 20, true).getBarcodes().getFirst();
		barcode.setReferenceEmail("customer@example.com");
		barcode.setStatus(ProductBarcodeStatus.CLAIMED);
		when(productBarcodeService.claimByReference("customer@example.com", "worker"))
				.thenReturn(barcode);

		mockMvc.perform(post("/api/barcodes/reference/customer@example.com/claim")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLAIMED"));
	}

	@Test
	void tipCreationRejectsAnonymousCaller() throws Exception {
		mockMvc.perform(post("/api/tips")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "workerId": 10,
						  "tipAmount": 100.00
						}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tipCreationAllowsWorkerCaller() throws Exception {
		when(tipService.createTip(any(TipRequest.class))).thenReturn(tipResponse(TipStatus.UNPAID));

		mockMvc.perform(post("/api/tips")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "workerId": 10,
						  "tipAmount": 100.00
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.workerId").value(10))
				.andExpect(jsonPath("$.status").value("UNPAID"));
	}

	@Test
	void paypalOnboardingRejectsWorkerCaller() throws Exception {
		mockMvc.perform(post("/api/tips/paypal/onboarding")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(paypalOnboardingJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void paypalOnboardingAllowsOwnerCaller() throws Exception {
		when(tipWithdrawalService.onboardPayPalAccount(any(PayPalAccountOnboardingRequest.class), eq("owner")))
				.thenReturn(new PayPalAccountResponse(
						5L,
						10L,
						1L,
						"worker@paypal.com",
						PayoutAccountStatus.ACTIVE,
						"https://app.example/paypal/onboarding",
						java.time.OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
						java.time.OffsetDateTime.parse("2026-06-23T10:00:00+02:00")));

		mockMvc.perform(post("/api/tips/paypal/onboarding")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(paypalOnboardingJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.workerId").value(10))
				.andExpect(jsonPath("$.ownerId").value(1));
	}

	@Test
	void withdrawalRequestRejectsWorkerCaller() throws Exception {
		mockMvc.perform(post("/api/tips/withdrawals")
				.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(withdrawalJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void withdrawalRequestAllowsOwnerCaller() throws Exception {
		when(tipWithdrawalService.requestWithdrawal(any(WithdrawalRequest.class), eq("owner")))
				.thenReturn(new WithdrawalResponse(
						77L,
						10L,
						1L,
						new BigDecimal("1098.00"),
						new MoneyResponse(new BigDecimal("1098.00"), SupportedCurrency.ZAR, "R", "R1,098.00"),
						2,
						"worker@paypal.com",
						TipWithdrawalStatus.REQUESTED,
						java.time.OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
						java.time.OffsetDateTime.parse("2026-06-23T10:00:00+02:00")));

		mockMvc.perform(post("/api/tips/withdrawals")
				.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(withdrawalJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.workerId").value(10))
				.andExpect(jsonPath("$.ownerId").value(1))
				.andExpect(jsonPath("$.status").value("REQUESTED"));
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
				  "password": "secret",
				  "jobTitle": "Cashier",
				  "tipQrCodeEnabled": true
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

	private String paypalOnboardingJson() {
		return """
				{
				  "workerId": 10,
				  "paypalEmail": "worker@paypal.com"
				}
				""";
	}

	private String withdrawalJson() {
		return """
				{
				  "workerId": 10
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

	private TipResponse tipResponse(TipStatus status) {
		return new TipResponse(
				42L,
				10L,
				new BigDecimal("100.00"),
				new BigDecimal("0.00"),
				new BigDecimal("100.00"),
				status,
				"plink_123",
				"https://pay.stripe.com/plink_123",
				"https://api.qrserver.com/v1/create-qr-code/?data=plink_123",
				java.time.OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
				java.time.OffsetDateTime.parse("2026-06-23T10:00:00+02:00"));
	}
}
