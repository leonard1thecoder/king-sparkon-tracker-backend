package com.king_sparkon_tracker.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.dto.MoneyResponse;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.OwnerPayPalAccountResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalEligibilityResponse;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalRequest;
import com.king_sparkon_tracker.backend.dto.TransactionWithdrawalResponse;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.PayoutAccountStatus;
import com.king_sparkon_tracker.backend.model.SupportedCurrency;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawalStatus;
import com.king_sparkon_tracker.backend.service.TransactionWithdrawalService;

class TransactionWithdrawalControllerTest {

	private TransactionWithdrawalService withdrawalService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		withdrawalService = mock(TransactionWithdrawalService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new TransactionWithdrawalController(withdrawalService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void onboardPayPalAccountReturnsOwnerPayoutAccount() throws Exception {
		when(withdrawalService.onboardPayPalAccount(any(OwnerPayPalAccountOnboardingRequest.class), eq("owner")))
				.thenReturn(new OwnerPayPalAccountResponse(
						5L,
						1L,
						3L,
						"owner@paypal.com",
						PayoutAccountStatus.ACTIVE,
						"https://app.example/paypal/onboarding?businessId=3&token=abc",
						OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
						OffsetDateTime.parse("2026-06-23T10:00:00+02:00")));

		mockMvc.perform(post("/api/transactions/withdrawals/paypal/onboarding")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "paypalEmail": "owner@paypal.com",
								  "callbackUrl": "https://app.example/paypal/callback"
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.ownerId").value(1))
				.andExpect(jsonPath("$.businessId").value(3))
				.andExpect(jsonPath("$.paypalEmail").value("owner@paypal.com"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void eligibilityReturnsWebsitePaymentWithdrawalBreakdown() throws Exception {
		when(withdrawalService.eligibility("owner")).thenReturn(new TransactionWithdrawalEligibilityResponse(
				1L,
				3L,
				new BigDecimal("1200.00"),
				money("1200.00", "R1,200.00"),
				new BigDecimal("78.00"),
				money("78.00", "R78.00"),
				new BigDecimal("6.50"),
				new BigDecimal("1122.00"),
				money("1122.00", "R1,122.00"),
				money("1000.00", "R1,000.00"),
				2,
				7,
				true,
				true,
				LocalDateTime.parse("2026-06-16T10:00:00")));

		mockMvc.perform(get("/api/transactions/withdrawals/eligibility")
						.principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ownerId").value(1))
				.andExpect(jsonPath("$.businessId").value(3))
				.andExpect(jsonPath("$.grossAmount").value(1200.00))
				.andExpect(jsonPath("$.feeAmount").value(78.00))
				.andExpect(jsonPath("$.feePercent").value(6.50))
				.andExpect(jsonPath("$.availableAmount").value(1122.00))
				.andExpect(jsonPath("$.eligibleTransactionCount").value(2))
				.andExpect(jsonPath("$.canWithdraw").value(true));
	}

	@Test
	void requestWithdrawalReturnsCreatedWithdrawal() throws Exception {
		when(withdrawalService.requestWithdrawal(any(TransactionWithdrawalRequest.class), eq("owner")))
				.thenReturn(withdrawalResponse());

		mockMvc.perform(post("/api/transactions/withdrawals")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "transactionIds": [10, 11]
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(77))
				.andExpect(jsonPath("$.grossAmount").value(1300.00))
				.andExpect(jsonPath("$.feeAmount").value(84.50))
				.andExpect(jsonPath("$.amount").value(1215.50))
				.andExpect(jsonPath("$.transactionCount").value(2))
				.andExpect(jsonPath("$.paypalEmail").value("owner@paypal.com"))
				.andExpect(jsonPath("$.status").value("REQUESTED"));

		verify(withdrawalService).requestWithdrawal(any(TransactionWithdrawalRequest.class), eq("owner"));
	}

	@Test
	void requestWithdrawalMapsBusinessRuleFailureToBadRequest() throws Exception {
		when(withdrawalService.requestWithdrawal(any(TransactionWithdrawalRequest.class), eq("owner")))
				.thenThrow(new IllegalArgumentException("One or more website payment transactions are not eligible for withdrawal"));

		mockMvc.perform(post("/api/transactions/withdrawals")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "transactionIds": [10, 11]
								}
								""")
						.principal(() -> "owner"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("One or more website payment transactions are not eligible for withdrawal"));
	}

	@Test
	void listWithdrawalsReturnsOwnerTransactionWithdrawals() throws Exception {
		when(withdrawalService.getWithdrawals("owner")).thenReturn(List.of(withdrawalResponse()));

		mockMvc.perform(get("/api/transactions/withdrawals")
						.principal(() -> "owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(77))
				.andExpect(jsonPath("$[0].businessId").value(3))
				.andExpect(jsonPath("$[0].amount").value(1215.50));
	}

	private TransactionWithdrawalResponse withdrawalResponse() {
		return new TransactionWithdrawalResponse(
				77L,
				1L,
				3L,
				new BigDecimal("1300.00"),
				money("1300.00", "R1,300.00"),
				new BigDecimal("84.50"),
				money("84.50", "R84.50"),
				new BigDecimal("6.50"),
				new BigDecimal("1215.50"),
				money("1215.50", "R1,215.50"),
				2,
				"owner@paypal.com",
				TransactionWithdrawalStatus.REQUESTED,
				OffsetDateTime.parse("2026-06-23T10:00:00+02:00"),
				OffsetDateTime.parse("2026-06-23T10:00:00+02:00"));
	}

	private MoneyResponse money(String amount, String formatted) {
		return new MoneyResponse(new BigDecimal(amount), SupportedCurrency.ZAR, "R", formatted);
	}
}
