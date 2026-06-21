package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.AlcoholReportResponse;
import com.king_sparkon_tracker.backend.dto.InventorySummaryResponse;
import com.king_sparkon_tracker.backend.dto.ProductMovementResponse;
import com.king_sparkon_tracker.backend.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Reports", description = "Owner-only business reports for stock, alcohol, and product movement.")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	/**
	 * Summarizes alcohol purchases and sales over an optional date range.
	 */
	@GetMapping("/alcohol")
	@Operation(summary = "Alcohol report", description = "Returns bought and sold quantities and values for alcohol products.")
	public AlcoholReportResponse alcoholReport(
			@Parameter(description = "Start date/time in ISO-8601 format.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "End date/time in ISO-8601 format.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(hidden = true) Principal principal) {
		return reportService.alcoholReport(from, to, principal.getName());
	}

	/**
	 * Gives owners a current stock valuation and low-stock count.
	 */
	@GetMapping("/inventory-summary")
	@Operation(summary = "Inventory summary", description = "Returns product counts, category counts, total stock value, and low-stock totals.")
	public InventorySummaryResponse inventorySummary(
			@Parameter(description = "Products at or below this stock quantity count as low stock.") @RequestParam(defaultValue = "5") int lowStockThreshold,
			@Parameter(hidden = true) Principal principal) {
		return reportService.inventorySummary(lowStockThreshold, principal.getName());
	}

	/**
	 * Ranks product movement by sold quantity for operational review.
	 */
	@GetMapping("/product-movement")
	@Operation(summary = "Product movement", description = "Returns bought and sold movement by product over an optional date range.")
	public List<ProductMovementResponse> productMovementReport(
			@Parameter(description = "Start date/time in ISO-8601 format.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "End date/time in ISO-8601 format.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(hidden = true) Principal principal) {
		return reportService.productMovementReport(from, to, principal.getName());
	}
}
