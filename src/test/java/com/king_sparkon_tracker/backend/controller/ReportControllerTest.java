package com.king_sparkon_tracker.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.dto.AlcoholReportResponse;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.service.ReportService;

class ReportControllerTest {

	private ReportService reportService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		reportService = mock(ReportService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ReportController(reportService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void alcoholReportReturnsReport() throws Exception {
		LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);
		when(reportService.alcoholReport(from, to, "owner")).thenReturn(new AlcoholReportResponse(
				from,
				to,
				10,
				5,
				new BigDecimal("150.00"),
				new BigDecimal("102.00")));

		mockMvc.perform(get("/api/reports/alcohol")
				.principal(() -> "owner")
				.param("from", "2026-01-01T00:00:00")
				.param("to", "2026-01-31T23:59:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.boughtQuantity").value(10))
				.andExpect(jsonPath("$.soldQuantity").value(5));
	}

	@Test
	void alcoholReportMapsInvalidDateRangeToBadRequest() throws Exception {
		when(reportService.alcoholReport(
				LocalDateTime.of(2026, 2, 1, 0, 0),
				LocalDateTime.of(2026, 1, 1, 0, 0),
				"owner"))
				.thenThrow(new IllegalArgumentException("Report from date cannot be after to date"));

		mockMvc.perform(get("/api/reports/alcohol")
				.principal(() -> "owner")
				.param("from", "2026-02-01T00:00:00")
				.param("to", "2026-01-01T00:00:00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Report from date cannot be after to date"));
	}
}
