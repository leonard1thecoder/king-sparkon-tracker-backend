package com.king_sparkon_tracker.backend.tips;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.controller.TipController;
import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.dto.PayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.PayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UpdateTipStatusRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.WithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.WithdrawalResponse;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.PayoutAccountStatus;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TipWithdrawalStatus;
import com.king_sparkon_tracker.backend.service.AiTipConfirmationService;
import com.king_sparkon_tracker.backend.service.TipService;
import com.king_sparkon_tracker.backend.service.TipWithdrawalService;

class TipControllerTest {

	private TipService tipService;
	private TipWithdrawalService withdrawalService;
	private AiTipConfirmationService aiTipConfirmationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		tipService = mock(TipService.class);
		withdrawalService = mock(TipWithdrawalService.class);
		aiTipConfirmationService = mock(AiTipConfirmationService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new TipController(tipService, withdrawalService, aiTipConfirmationService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void createTipReturnsCreatedTipPaymentContract() throws Exception {
		when(tipService.createTip(any(TipRequest.class))).thenReturn(tipResponse(TipStatus.UNPAID));

		mockMvc.perform(post("/api/tips")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "workerId": 10,
								  "tipAmount": 100.00,
								  "callbackUrl": "https://app.example/tips/return"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.workerId").value(10))
				.andExpect(jsonPath("$.tipAmount").value(100.00))
				.andExpect(jsonPath("$.systemFee").value(0.00))
				.andExpect(jsonPath("$.netAmount").value(100.00))
				.andExpect(jsonPath("$.status").value("UNPAID"))
				.andExpect(jsonPath("$.paymentUrl").value("https://pay.stripe.com/plink_123"))
				.andExpect(jsonPath("$.qrCodeUrl").value("https://api.qrserver.com/v1/create-qr-code/?data=plink_123"));

		verify(tipService).createTip(any(TipRequest.class));
	}

	@Test
	void updateTipStatusReturnsPaidTip() throws Exception {
		when(tipService.updateTipStatus(eq(42L), any(UpdateTipStatusRequest.class), eq("owner")))
				.thenReturn(tipResponse(TipStatus.PAID));

		mockMvc.perform(patch("/api/tips/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "PAID"
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	@Test
	void updateTipStatusMapsInvalidTransitionToBadRequest() throws Exception {
		when(tipService.updateTipStatus(eq(42L), any(UpdateTipStatusRequest.class), eq("owner")))
				.thenThrow(new IllegalArgumentException("Only UNPAID tips can be marked as PAID"));

		mockMvc.perform(patch("/api/tips/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "PAID"
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Only UNPAID tips can be marked as PAID"));
	}

	@Test
	void getTipsForWorkerReturnsWorkerTips() throws Exception {
		when(tipService.getTipsForWorker(10L, "owner")).thenReturn(List.of(tipResponse(TipStatus.PAID)));

		mockMvc.perform(get("/api/tips/worker/10").principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].workerId").value(10))
				.andExpect(jsonPath("$[0].status").value("PAID"));
	}

	@Test
	void getMyWorkerTipsReturnsCurrentWorkerTips() throws Exception {
		when(tipService.getTipsForCurrentWorker("worker")).thenReturn(List.of(tipResponse(TipStatus.PAID)));

		mockMvc.perform(get("/api/tips/me").principal(() -> "worker"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].workerId").value(10))
				.andExpect(jsonPath("$[0].status").value("PAID"));
	}

	@Test
	void getTipsByStatusReturnsFilteredTips() throws Exception {
		when(tipService.getTipsByStatus(TipStatus.UNPAID, "owner"))
				.thenReturn(List.of(tipResponse(TipStatus.UNPAID)));

		mockMvc.perform(get("/api/tips")
						.param("status", "UNPAID")
						.principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("UNPAID"));
	}

	@Test
	void onboardPayPalAccountReturnsOnboardingLink() throws Exception {
		when(withdrawalService.onboardPayPalAccount(any(PayPalAccountOnboardingRequest.class), eq("owner")))
				.thenReturn(new PayPalAccountResponse(
						5L,
						10L,
						1L,
						"worker@paypal.com",
						PayoutAccountStatus.ACTIVE,
						"https://app.example/paypal/onboarding?workerId=10&token=abc",
						OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
						OffsetDateTime.parse("2026-06-23T10:00:00+02:00")));

		mockMvc.perform(post("/api/tips/paypal/onboarding")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "workerId": 10,
								  "paypalEmail": "worker@paypal.com"
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.workerId").value(10))
				.andExpect(jsonPath("$.ownerId").value(1))
				.andExpect(jsonPath("$.paypalEmail").value("worker@paypal.com"))
				.andExpect(jsonPath("$.onboardingUrl").value("https://app.example/paypal/onboarding?workerId=10&token=abc"));
	}

	@Test
	void withdrawalEligibilityReturnsMinimumAndBalance() throws Exception {
		when(withdrawalService.eligibility(10L, "owner")).thenReturn(new WithdrawalEligibilityResponse(
				10L,
				new BigDecimal("1098.00"),
				money("1098.00", "R1,098.00"),
				money("1000.00", "R1,000.00"),
				2,
				7,
				true,
				true,
				OffsetDateTime.parse("2026-06-16T10:00:00+02:00")));

		mockMvc.perform(get("/api/tips/worker/10/withdrawals/eligibility")
						.principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.workerId").value(10))
				.andExpect(jsonPath("$.canWithdraw").value(true))
				.andExpect(jsonPath("$.holdDays").value(7))
				.andExpect(jsonPath("$.localizedMinimumAmount.formatted").value("R1,000.00"));
	}

	@Test
	void requestWithdrawalReturnsCreatedWithdrawal() throws Exception {
		when(withdrawalService.requestWithdrawal(any(WithdrawalRequest.class), eq("owner"))).thenReturn(new WithdrawalResponse(
				77L,
				10L,
				1L,
				new BigDecimal("1098.00"),
				money("1098.00", "R1,098.00"),
				2,
				"worker@paypal.com",
				TipWithdrawalStatus.REQUESTED,
				OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
				OffsetDateTime.parse("2026-06-23T10:00:00+02:00")));

		mockMvc.perform(post("/api/tips/withdrawals")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "workerId": 10
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(77))
				.andExpect(jsonPath("$.ownerId").value(1))
				.andExpect(jsonPath("$.amount").value(1098.00))
				.andExpect(jsonPath("$.tipCount").value(2))
				.andExpect(jsonPath("$.status").value("REQUESTED"));
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
				OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
				OffsetDateTime.parse("2026-06-23T10:00:00+02:00"));
	}

	private MoneyResponse money(String amount, String formatted) {
		return new MoneyResponse(new BigDecimal(amount), SupportedCurrency.ZAR, "R", formatted);
	}
}
