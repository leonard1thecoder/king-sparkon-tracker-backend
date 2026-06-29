package com.king_sparkon_tracker.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UserDashboardDtos.BusinessCardResponse;
import com.king_sparkon_tracker.backend.dto.UserDashboardDtos.UserDashboardResponse;
import com.king_sparkon_tracker.backend.dto.UserDashboardDtos.WorkerTipCardResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;

@Service
@Transactional(readOnly = true)
public class UserDashboardService {

	private final BusinessRepository businessRepository;
	private final TrackerUserRepository trackerUserRepository;
	private final TicketEventRepository ticketEventRepository;
	private final TicketManagementService ticketManagementService;
	private final TipService tipService;

	public UserDashboardService(
			BusinessRepository businessRepository,
			TrackerUserRepository trackerUserRepository,
			TicketEventRepository ticketEventRepository,
			TicketManagementService ticketManagementService,
			TipService tipService) {
		this.businessRepository = businessRepository;
		this.trackerUserRepository = trackerUserRepository;
		this.ticketEventRepository = ticketEventRepository;
		this.ticketManagementService = ticketManagementService;
		this.tipService = tipService;
	}

	public UserDashboardResponse dashboard() {
		List<BusinessCardResponse> businesses = businesses();
		List<TicketEventResponse> events = upcomingEventsAcrossBusinesses();
		return new UserDashboardResponse(businesses, events);
	}

	public List<BusinessCardResponse> businesses() {
		return businessRepository.findTop50ByOrderByModifiedDateDesc()
				.stream()
				.map(BusinessCardResponse::from)
				.toList();
	}

	public List<WorkerTipCardResponse> workersForBusiness(Long businessId) {
		Business business = business(businessId);
		return trackerUserRepository.findByBusiness_IdAndPrivilege_NameOrderByUsernameAsc(business.getId(), PrivilegeRole.Worker)
				.stream()
				.map(WorkerTipCardResponse::from)
				.toList();
	}

	public List<TicketEventResponse> eventsForBusiness(Long businessId) {
		Business business = business(businessId);
		return eventOwnerKeys(business).stream()
				.flatMap(ownerId -> ticketEventRepository.findByOwnerIdAndStatusInAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(
						ownerId,
						List.of(TicketEventStatus.PUBLISHED),
						LocalDate.now()).stream())
				.collect(LinkedHashMap::new, (map, event) -> map.putIfAbsent(event.getId(), event), Map::putAll)
				.values()
				.stream()
				.map(event -> ticketManagementService.getEventById(event.getId()))
				.toList();
	}

	@Transactional
	public TipResponse tipWorker(TipRequest request) {
		return tipService.createTip(request);
	}

	private List<TicketEventResponse> upcomingEventsAcrossBusinesses() {
		return businessRepository.findTop50ByOrderByModifiedDateDesc().stream()
				.flatMap(business -> eventsForBusiness(business.getId()).stream())
				.limit(50)
				.toList();
	}

	private Business business(Long businessId) {
		return businessRepository.findById(businessId)
				.orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
	}

	private List<String> eventOwnerKeys(Business business) {
		List<String> keys = new ArrayList<>();
		if (business.getOwner() != null) {
			if (business.getOwner().getId() != null) {
				keys.add(String.valueOf(business.getOwner().getId()));
			}
			if (business.getOwner().getUsername() != null && !business.getOwner().getUsername().isBlank()) {
				keys.add(business.getOwner().getUsername());
			}
		}
		return keys.stream().distinct().toList();
	}
}
