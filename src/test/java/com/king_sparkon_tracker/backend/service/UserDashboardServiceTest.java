package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.tickets.dto.TicketDtos.TicketEventResponse;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import com.king_sparkon_tracker.backend.tickets.service.TicketManagementService;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

	@Mock
	private BusinessRepository businessRepository;

	@Mock
	private TrackerUserRepository trackerUserRepository;

	@Mock
	private TicketEventRepository ticketEventRepository;

	@Mock
	private TicketManagementService ticketManagementService;

	@Mock
	private TipService tipService;

	@Mock
	private Business business;

	@Mock
	private TrackerUser owner;

	private UserDashboardService service;

	@BeforeEach
	void setUp() {
		service = new UserDashboardService(
				businessRepository,
				trackerUserRepository,
				ticketEventRepository,
				ticketManagementService,
				tipService);
	}

	@Test
	void eventsForBusinessReturnsPublishedEventsOnceWhenOwnerHasIdAndUsername() {
		TicketEvent event = new TicketEvent();
		event.setId("event-1");
		TicketEventResponse response = new TicketEventResponse(
				"event-1",
				"owner",
				"Launch Party",
				"Opening night",
				"Johannesburg",
				LocalDate.now().plusDays(1),
				LocalTime.NOON,
				null,
				null,
				TicketEventStatus.PUBLISHED,
				List.of(),
				List.of(),
				List.of(),
				null,
				null);

		when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
		when(business.getOwner()).thenReturn(owner);
		when(owner.getId()).thenReturn(7L);
		when(owner.getUsername()).thenReturn("owner");
		when(ticketEventRepository.findByOwnerIdAndStatusInAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(
				any(), any(), any()))
				.thenReturn(List.of(event));
		when(ticketManagementService.getEventById("event-1")).thenReturn(response);

		List<TicketEventResponse> events = service.eventsForBusiness(1L);

		assertThat(events).containsExactly(response);
	}

	@Test
	void tipWorkerDelegatesToTipPaymentFlowAndReturnsStripePaymentUrl() {
		TipRequest request = new TipRequest(15L, new BigDecimal("50.00"), "https://app.example.com/tips/success", "client@example.com");
		TipResponse response = new TipResponse(
				1L,
				15L,
				new BigDecimal("50.00"),
				BigDecimal.ZERO,
				new BigDecimal("50.00"),
				TipStatus.UNPAID,
				"plink_123",
				"https://stripe.example.com/pay/plink_123",
				"https://qr.example.com/plink_123",
				null,
				null);
		when(tipService.createTip(request)).thenReturn(response);

		TipResponse actual = service.tipWorker(request);

		assertThat(actual.paymentUrl()).contains("stripe.example.com");
		verify(tipService).createTip(request);
	}
}
