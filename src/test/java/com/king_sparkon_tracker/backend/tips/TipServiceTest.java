package com.king_sparkon_tracker.backend.tips;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UpdateTipStatusRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.service.NotificationService;
import com.king_sparkon_tracker.backend.service.StripeService;
import com.king_sparkon_tracker.backend.service.SubscriberService;
import com.king_sparkon_tracker.backend.service.TipService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

@ExtendWith(MockitoExtension.class)
class TipServiceTest {

	@Mock
	private TipRepository tipRepository;

	@Mock
	private TrackerUserService trackerUserService;

	@Mock
	private StripeService stripeService;

	@Mock
	private NotificationService notificationService;

	@Mock
	private SubscriberService subscriberService;

	private TipService tipService;

	@BeforeEach
	void setUp() {
		tipService = new TipService(tipRepository, trackerUserService, stripeService, notificationService, subscriberService);
	}

	@Test
	void createTipPersistsUnpaidTipCreatesStripePaymentLinkAndReturnsBreakdown() {
		stubTipSave();
		when(trackerUserService.getUserById(10L)).thenReturn(worker(10L));
		when(stripeService.createTipPaymentLink(
				any(Tip.class),
				eq(new BigDecimal("0.00")),
				eq(new BigDecimal("100.00")),
				eq("https://app.example/tips/return")))
				.thenReturn(new StripeService.CreatedTipPaymentLink(
						"plink_123",
						"https://pay.stripe.com/plink_123",
						"https://api.qrserver.com/v1/create-qr-code/?data=plink_123"));

		TipResponse response = tipService.createTip(new TipRequest(
				10L,
				new BigDecimal("100.00"),
				"https://app.example/tips/return",
				"+27821234567"));

		assertThat(response.id()).isEqualTo(42L);
		assertThat(response.workerId()).isEqualTo(10L);
		assertThat(response.tipAmount()).isEqualByComparingTo("100.00");
		assertThat(response.systemFee()).isEqualByComparingTo("0.00");
		assertThat(response.netAmount()).isEqualByComparingTo("100.00");
		assertThat(response.status()).isEqualTo(TipStatus.UNPAID);
		assertThat(response.paymentReference()).isEqualTo("plink_123");
		assertThat(response.paymentUrl()).isEqualTo("https://pay.stripe.com/plink_123");
		assertThat(response.qrCodeUrl()).contains("qrserver");

		ArgumentCaptor<Tip> tipCaptor = ArgumentCaptor.forClass(Tip.class);
		verify(tipRepository, org.mockito.Mockito.times(2)).save(tipCaptor.capture());
		assertThat(tipCaptor.getAllValues().get(1).getPaymentReference()).isEqualTo("plink_123");
		verify(notificationService).logTipPaymentLink(any(Tip.class), eq("https://pay.stripe.com/plink_123"));
		verify(subscriberService).subscribeTipPaymentClient("+27821234567");
	}

	@Test
	void createTipRejectsWorkerWithoutTipsPrivilege() {
		TrackerUser worker = worker(11L);
		worker.updateWorkerProfile("Cashier", false);
		when(trackerUserService.getUserById(11L)).thenReturn(worker);

		assertThatThrownBy(() -> tipService.createTip(new TipRequest(
				11L,
				new BigDecimal("50.00"),
				"https://app.example/tips/return",
				"+27821234567")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not privileged to receive tips");
	}

	@Test
	void updateTipStatusTransitionsUnpaidTipToPaid() {
		stubTipSave();
		Tip tip = new Tip(worker(10L), new BigDecimal("100.00"));
		ReflectionTestUtils.setField(tip, "id", 7L);
		when(tipRepository.findById(7L)).thenReturn(Optional.of(tip));

		TipResponse response = tipService.updateTipStatus(7L, new UpdateTipStatusRequest(TipStatus.PAID));

		assertThat(response.status()).isEqualTo(TipStatus.PAID);
		verify(tipRepository).save(tip);
	}

	@Test
	void updateTipStatusRejectsUnsupportedTargetStatus() {
		Tip tip = new Tip(worker(10L), new BigDecimal("100.00"));
		when(tipRepository.findById(7L)).thenReturn(Optional.of(tip));

		assertThatThrownBy(() -> tipService.updateTipStatus(7L, new UpdateTipStatusRequest(TipStatus.UNPAID)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Only PAID status updates are supported");
	}

	@Test
	void updateTipStatusRejectsPaidToPaidTransition() {
		Tip tip = new Tip(worker(10L), new BigDecimal("100.00"));
		tip.markPaid();
		when(tipRepository.findById(7L)).thenReturn(Optional.of(tip));

		assertThatThrownBy(() -> tipService.updateTipStatus(7L, new UpdateTipStatusRequest(TipStatus.PAID)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Only UNPAID tips can be marked as PAID");
	}

	@Test
	void updateTipStatusThrowsWhenTipIsMissing() {
		when(tipRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> tipService.updateTipStatus(404L, new UpdateTipStatusRequest(TipStatus.PAID)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Tip not found: 404");
	}

	@Test
	void getTipsForWorkerMapsFinancialBreakdown() {
		Tip tip = new Tip(worker(10L), new BigDecimal("200.00"));
		when(tipRepository.findByWorker_IdOrderByCreatedDesc(10L)).thenReturn(List.of(tip));

		List<TipResponse> responses = tipService.getTipsForWorker(10L);

		assertThat(responses).hasSize(1);
		assertThat(responses.getFirst().systemFee()).isEqualByComparingTo("0.00");
		assertThat(responses.getFirst().netAmount()).isEqualByComparingTo("200.00");
	}

	private void stubTipSave() {
		when(tipRepository.save(any(Tip.class))).thenAnswer(invocation -> {
			Tip tip = invocation.getArgument(0);
			if (tip.getId() == null) {
				ReflectionTestUtils.setField(tip, "id", 42L);
			}
			if (tip.getCreated() == null) {
				ReflectionTestUtils.setField(tip, "created", OffsetDateTime.parse("2026-06-23T10:00:00+02:00"));
			}
			if (tip.getUpdated() == null) {
				ReflectionTestUtils.setField(tip, "updated", OffsetDateTime.parse("2026-06-23T10:00:00+02:00"));
			}
			return tip;
		});
	}

	private TrackerUser worker(Long id) {
		TrackerUser worker = new TrackerUser(
				"worker-" + id,
				"worker-" + id + "@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Worker));
		worker.updateWorkerProfile("Worker", true);
		ReflectionTestUtils.setField(worker, "id", id);
		return worker;
	}
}
