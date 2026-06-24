package com.king_sparkon_tracker.backend.controller;

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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.king_sparkon_tracker.backend.dto.AffiliateLinkResponse;
import com.king_sparkon_tracker.backend.dto.CreateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.dto.UpdateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.exception.ApiExceptionHandler;
import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.service.AffiliateLinkService;

class AffiliateLinkControllerTest {

	private AffiliateLinkService affiliateLinkService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		affiliateLinkService = mock(AffiliateLinkService.class);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new AffiliateLinkController(affiliateLinkService))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void createAffiliateLink() throws Exception {
		when(affiliateLinkService.create(any(CreateAffiliateLinkRequest.class))).thenReturn(response(1L));

		mockMvc.perform(post("/api/affiliate-links")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Barcode scanner deal",
								  "description": "Recommended scanner",
								  "affiliateUrl": "https://example.com/scanner?ref=king",
								  "imageUrl": "https://example.com/scanner.png",
								  "websiteName": "Example Store",
								  "category": "BARCODE_SCANNER",
								  "placement": "OWNER_ADD_PRODUCT",
								  "priority": 5,
								  "displayPlans": ["FREE_TRIAL", "PLUS"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Barcode scanner deal"));
	}

	@Test
	void randomForAuthenticatedUserUsesPrincipal() throws Exception {
		when(affiliateLinkService.randomForAuthenticated(
				eq(AffiliatePlacement.WORKER_BARCODE_THRESHOLD),
				eq(3),
				eq(2),
				eq("worker")))
				.thenReturn(List.of(response(2L)));

		mockMvc.perform(get("/api/affiliate-links/random")
						.param("placement", "WORKER_BARCODE_THRESHOLD")
						.param("triggerCount", "3")
						.param("limit", "2")
						.principal(principal("worker")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(2));
	}

	@Test
	void publicRandomSupportsWorkerId() throws Exception {
		when(affiliateLinkService.randomForPublicClient(
				eq(AffiliatePlacement.TIP_CLIENT_QR_SCAN),
				eq(null),
				eq(1),
				eq(99L),
				eq(null)))
				.thenReturn(List.of(response(3L)));

		mockMvc.perform(get("/api/affiliate-links/public/random")
						.param("placement", "TIP_CLIENT_QR_SCAN")
						.param("workerId", "99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(3));
	}

	@Test
	void recordClick() throws Exception {
		when(affiliateLinkService.recordClick(7L)).thenReturn(response(7L));

		mockMvc.perform(post("/api/affiliate-links/7/click"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7));

		verify(affiliateLinkService).recordClick(7L);
	}

	@Test
	void updateAffiliateLink() throws Exception {
		when(affiliateLinkService.update(eq(8L), any(UpdateAffiliateLinkRequest.class))).thenReturn(response(8L));

		mockMvc.perform(patch("/api/affiliate-links/8")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "INACTIVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(8));
	}

	private Principal principal(String name) {
		return () -> name;
	}

	private AffiliateLinkResponse response(Long id) {
		return new AffiliateLinkResponse(
				id,
				"Barcode scanner deal",
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
				LocalDateTime.parse("2026-06-24T00:00:00"),
				LocalDateTime.parse("2026-06-24T00:00:00"));
	}
}
