package com.king_sparkon_tracker.backend.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.service.GoogleStorageService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.FaceDecision;
import com.king_sparkon_tracker.backend.tickets.dto.TicketIdentityDtos.GateVerificationRequest;
import com.king_sparkon_tracker.backend.tickets.repository.TicketAuditLogRepository;
import com.king_sparkon_tracker.backend.tickets.repository.UserTicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketIdentityLockingTest {

	@Mock private UserTicketRepository userTicketRepository;
	@Mock private TicketManagementService ticketManagementService;
	@Mock private TicketAuditLogRepository ticketAuditLogRepository;
	@Mock private TrackerUserService trackerUserService;
	@Mock private GoogleStorageService googleStorageService;

	private TicketIdentityService service;

	@BeforeEach
	void setUp() {
		service = new TicketIdentityService(
				userTicketRepository,
				ticketManagementService,
				ticketAuditLogRepository,
				trackerUserService,
				googleStorageService);
		when(trackerUserService.getUserByUsername("worker")).thenReturn(
				new TrackerUser("worker", "worker@example.com", "encoded", new Privilege(PrivilegeRole.Worker)));
	}

	@Test
	void qrVerificationUsesPessimisticLookupBeforeAnyStateTransition() {
		when(userTicketRepository.findLockedByQrCodeValue("qr-123")).thenReturn(Optional.empty());

		var response = service.verifyByQr(
				new GateVerificationRequest("qr-123", null, FaceDecision.MATCH),
				"worker");

		assertThat(response.valid()).isFalse();
		verify(userTicketRepository).findLockedByQrCodeValue("qr-123");
		verify(userTicketRepository, never()).findByQrCodeValue("qr-123");
	}

	@Test
	void referenceVerificationUsesPessimisticLookupBeforeAnyStateTransition() {
		when(userTicketRepository.findLockedByTicketReferenceIgnoreCase("TICKET-123")).thenReturn(Optional.empty());

		var response = service.verifyByReference(
				new GateVerificationRequest("TICKET-123", null, FaceDecision.MATCH),
				"worker");

		assertThat(response.valid()).isFalse();
		verify(userTicketRepository).findLockedByTicketReferenceIgnoreCase("TICKET-123");
		verify(userTicketRepository, never()).findByTicketReferenceIgnoreCase("TICKET-123");
	}
}
