package com.king_sparkon_tracker.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UserDashboardDtos.BusinessCardResponse;
import com.king_sparkon_tracker.backend.dto.UserDashboardDtos.UserDashboardResponse;
import com.king_sparkon_tracker.backend.dto.UserDashboardDtos.WorkerTipCardResponse;
import com.king_sparkon_tracker.backend.service.UserDashboardService;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user-dashboard")
@Tag(name = "User Dashboard", description = "Customer-facing dashboard for businesses, workers, tips, and ticket events.")
public class UserDashboardController {

	private final UserDashboardService userDashboardService;

	public UserDashboardController(UserDashboardService userDashboardService) {
		this.userDashboardService = userDashboardService;
	}

	@GetMapping
	@Operation(summary = "User dashboard", description = "Returns visible businesses and upcoming ticket events for a user dashboard.")
	public UserDashboardResponse dashboard() {
		return userDashboardService.dashboard();
	}

	@GetMapping("/businesses")
	@Operation(summary = "Visible businesses", description = "Lists businesses that users can view from their dashboard.")
	public List<BusinessCardResponse> businesses() {
		return userDashboardService.businesses();
	}

	@GetMapping("/businesses/{businessId}/workers")
	@Operation(summary = "Business workers", description = "Lists workers for tips, including profile pictures and tip QR URLs.")
	public List<WorkerTipCardResponse> workers(@PathVariable Long businessId) {
		return userDashboardService.workersForBusiness(businessId);
	}

	@GetMapping("/businesses/{businessId}/events")
	@Operation(summary = "Business events", description = "Lists published ticket events for the selected business.")
	public List<TicketEventResponse> events(@PathVariable Long businessId) {
		return userDashboardService.eventsForBusiness(businessId);
	}

	@PostMapping("/tips")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Tip a worker", description = "Creates a worker tip payment link from the user dashboard.")
	public TipResponse tipWorker(@Valid @RequestBody TipRequest request) {
		return userDashboardService.tipWorker(request);
	}
}
