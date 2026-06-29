package com.king_sparkon_tracker.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.king_sparkon_tracker.backend.config.CacheConfig;
import com.king_sparkon_tracker.backend.config.CorsConfig;
import com.king_sparkon_tracker.backend.config.JacksonConfig;
import com.king_sparkon_tracker.backend.controller.AffiliateLinkController;
import com.king_sparkon_tracker.backend.controller.AuthenticationController;
import com.king_sparkon_tracker.backend.controller.ProductController;
import com.king_sparkon_tracker.backend.controller.TipController;
import com.king_sparkon_tracker.backend.controller.UserController;
import com.king_sparkon_tracker.backend.dto.AffiliateLinkResponse;
import com.king_sparkon_tracker.backend.dto.CreateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.service.AffiliateLinkService;
import com.king_sparkon_tracker.backend.service.BusinessAccessService;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.OnboardingProfileService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;
import com.king_sparkon_tracker.backend.service.PriceLocalizationService;
import com.king_sparkon_tracker.backend.service.ProductPricingService;
import com.king_sparkon_tracker.backend.service.ProductService;
import com.king_sparkon_tracker.backend.service.RefreshTokenService;
import com.king_sparkon_tracker.backend.service.TipService;
import com.king_sparkon_tracker.backend.service.TipWithdrawalService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

@WebMvcTest(controllers = {
		AuthenticationController.class,
		UserController.class,
		ProductController.class,
		TipController.class,
		AffiliateLinkController.class
})
@Import({ SecurityConfig.class, CorsConfig.class, JacksonConfig.class, CacheConfig.class, ApiExceptionHandler.class })
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TrackerUserService userService;

	@MockitoBean
	private OnboardingProfileService onboardingProfileService;

	@MockitoBean
	private RefreshTokenService refreshTokenService;

	@MockitoBean
	private PasswordResetService passwordResetService;

	@MockitoBean
	private EmailVerificationService emailVerificationService;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private ProductPricingService productPricingService;

	@MockitoBean
	private PriceLocalizationService priceLocalizationService;

	@MockitoBean
	private TipService tipService;

	@MockitoBean
	private TipWithdrawalService tipWithdrawalService;

	@MockitoBean
	private AffiliateLinkService affiliateLinkService;

	@MockitoBean
	private BusinessAccessService businessAccessService;

	@MockitoBean
	private RateLimitService rateLimitService;

	@MockitoBean
	private DataSource dataSource;

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
				.andExpect(jsonPath("$.username").value("worker"));
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
	void tipCreationAllowsWorkerCaller() throws Exception {
		when(tipService.createTip(any(TipRequest.class))).thenReturn(tipResponse());

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
				.andExpect(jsonPath("$.workerId").value(10));
	}

	@Test
	void affiliatePublicRandomAdsArePublic() throws Exception {
		when(affiliateLinkService.randomForPublicClient(
				eq(AffiliatePlacement.TIP_CLIENT_QR_SCAN),
				eq(null),
				eq(1),
				eq(99L),
				eq(null)))
				.thenReturn(List.of(affiliateLinkResponse(1L)));

		mockMvc.perform(get("/api/affiliate-links/public/random")
						.param("placement", "TIP_CLIENT_QR_SCAN")
						.param("workerId", "99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void affiliateRandomAdsRequireAuthenticatedCaller() throws Exception {
		mockMvc.perform(get("/api/affiliate-links/random")
						.param("placement", "OWNER_ADD_PRODUCT"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void affiliateLinkCreationAllowsOwnerCaller() throws Exception {
		when(affiliateLinkService.create(any(CreateAffiliateLinkRequest.class))).thenReturn(affiliateLinkResponse(2L));

		mockMvc.perform(post("/api/affiliate-links")
						.with(user("owner").authorities(() -> PrivilegeRole.Owner.name()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Scanner deal",
								  "affiliateUrl": "https://example.com/scanner?ref=king",
								  "websiteName": "Example Store",
								  "placement": "OWNER_ADD_PRODUCT",
								  "displayPlans": ["FREE_TRIAL", "PLUS"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(2));
	}

	@Test
	void affiliateLinkCreationRejectsWorkerCaller() throws Exception {
		mockMvc.perform(post("/api/affiliate-links")
						.with(user("worker").authorities(() -> PrivilegeRole.Worker.name()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Scanner deal",
								  "affiliateUrl": "https://example.com/scanner?ref=king",
								  "websiteName": "Example Store",
								  "placement": "OWNER_ADD_PRODUCT"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void clickTrackingIsPublic() throws Exception {
		when(affiliateLinkService.recordClick(7L)).thenReturn(affiliateLinkResponse(7L));

		mockMvc.perform(post("/api/affiliate-links/7/click"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7));
	}

	private TrackerUser trackerUser(String username, String emailAddress, PrivilegeRole role) {
		return new TrackerUser(username, emailAddress, "encoded", new Privilege(role));
	}

	private String workerJson() {
		return """
				{
				  "username": "worker",
				  "emailAddress": "worker@example.com",
				  "password": "secret",
				  "jobTitle": "Cashier"
				}
				""";
	}

	private String productJson() {
		return """
				{
				  "name": "Water",
				  "category": "NonAlcohol",
				  "price": 20.00,
				  "stockQuantity": 20
				}
				""";
	}

	private TipResponse tipResponse() {
		return new TipResponse(
				1L,
				10L,
				100.00,
				8.50,
				91.50,
				LocalDateTime.parse("2026-06-24T10:00:00"),
				LocalDateTime.parse("2026-06-24T10:00:00"),
				TipStatus.UNPAID,
				"stripe-ref",
				"https://pay.stripe.com/test",
				"https://example.com/qr.png",
				SupportedCurrency.ZAR,
				"R100.00",
				"R8.50",
				"R91.50");
	}

	private AffiliateLinkResponse affiliateLinkResponse(Long id) {
		return new AffiliateLinkResponse(
				id,
				"Scanner deal",
				"Recommended scanner",
				"https://example.com/scanner?ref=king",
				"https://example.com/scanner.png",
				"Example Store",
				"BARCODE_SCANNER",
				AffiliatePlacement.OWNER_ADD_PRODUCT,
				AffiliateLinkStatus.ACTIVE,
				5,
				List.of(BusinessPlan.FREE_TRIAL, BusinessPlan.PLUS),
				10,
				2,
				LocalDateTime.parse("2026-06-24T10:00:00"),
				LocalDateTime.parse("2026-06-24T10:00:00"));
	}
}
