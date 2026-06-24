package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.CreateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.model.AffiliateLink;
import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.AffiliateLinkRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

class AffiliateLinkServiceTest {

	private AffiliateLinkRepository affiliateLinkRepository;
	private TrackerUserRepository userRepository;
	private BusinessRepository businessRepository;
	private AffiliateLinkService service;

	@BeforeEach
	void setUp() {
		affiliateLinkRepository = mock(AffiliateLinkRepository.class);
		userRepository = mock(TrackerUserRepository.class);
		businessRepository = mock(BusinessRepository.class);
		service = new AffiliateLinkService(affiliateLinkRepository, userRepository, businessRepository);
	}

	@Test
	void createDefaultsAdsToFreeTrialAndPlusOnly() {
		when(affiliateLinkRepository.save(any(AffiliateLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(new CreateAffiliateLinkRequest(
				"Barcode Scanner Deal",
				"Recommended scanner",
				"https://example.com/scanner?ref=king",
				"https://example.com/scanner.png",
				"Example Store",
				"BARCODE_SCANNER",
				AffiliatePlacement.OWNER_ADD_PRODUCT,
				AffiliateLinkStatus.ACTIVE,
				5,
				null));

		assertThat(response.displayPlans()).containsExactly(BusinessPlan.FREE_TRIAL, BusinessPlan.PLUS);
		assertThat(response.priority()).isEqualTo(5);
	}

	@Test
	void workerBarcodeAdsRespectFreeTrialAndPlusThresholds() {
		AffiliateLink link = link(AffiliatePlacement.WORKER_BARCODE_THRESHOLD, List.of(BusinessPlan.FREE_TRIAL, BusinessPlan.PLUS));
		when(affiliateLinkRepository.findByStatusAndPlacement(AffiliateLinkStatus.ACTIVE, AffiliatePlacement.WORKER_BARCODE_THRESHOLD))
				.thenReturn(List.of(link));
		when(affiliateLinkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		TrackerUser freeWorker = workerWithPlan(1L, BusinessPlan.FREE_TRIAL);
		when(userRepository.findByUsername("free-worker")).thenReturn(Optional.of(freeWorker));

		assertThat(service.randomForAuthenticated(AffiliatePlacement.WORKER_BARCODE_THRESHOLD, 2, 1, "free-worker")).isEmpty();
		assertThat(service.randomForAuthenticated(AffiliatePlacement.WORKER_BARCODE_THRESHOLD, 3, 1, "free-worker")).hasSize(1);

		TrackerUser plusWorker = workerWithPlan(2L, BusinessPlan.PLUS);
		when(userRepository.findByUsername("plus-worker")).thenReturn(Optional.of(plusWorker));

		assertThat(service.randomForAuthenticated(AffiliatePlacement.WORKER_BARCODE_THRESHOLD, 7, 1, "plus-worker")).isEmpty();
		assertThat(service.randomForAuthenticated(AffiliatePlacement.WORKER_BARCODE_THRESHOLD, 8, 1, "plus-worker")).hasSize(1);
	}

	@Test
	void proPlanDoesNotReceiveAds() {
		TrackerUser proOwner = workerWithPlan(3L, BusinessPlan.PRO);
		when(userRepository.findByUsername("pro-owner")).thenReturn(Optional.of(proOwner));

		assertThat(service.randomForAuthenticated(AffiliatePlacement.OWNER_ADD_PRODUCT, null, 1, "pro-owner")).isEmpty();
	}

	@Test
	void publicClientAdsCanResolvePlanFromWorkerId() {
		AffiliateLink link = link(AffiliatePlacement.TIP_CLIENT_QR_SCAN, List.of(BusinessPlan.PLUS));
		when(affiliateLinkRepository.findByStatusAndPlacement(AffiliateLinkStatus.ACTIVE, AffiliatePlacement.TIP_CLIENT_QR_SCAN))
				.thenReturn(List.of(link));
		when(affiliateLinkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.findById(99L)).thenReturn(Optional.of(workerWithPlan(99L, BusinessPlan.PLUS)));

		assertThat(service.randomForPublicClient(AffiliatePlacement.TIP_CLIENT_QR_SCAN, null, 1, 99L, BusinessPlan.FREE_TRIAL))
				.hasSize(1);
	}

	@Test
	void recordClickIncrementsClickCounter() {
		AffiliateLink link = link(AffiliatePlacement.OWNER_ADD_WORKER, List.of(BusinessPlan.FREE_TRIAL));
		when(affiliateLinkRepository.findById(5L)).thenReturn(Optional.of(link));
		when(affiliateLinkRepository.save(link)).thenReturn(link);

		var response = service.recordClick(5L);

		assertThat(response.clickCount()).isEqualTo(1);
		verify(affiliateLinkRepository).save(link);
	}

	private AffiliateLink link(AffiliatePlacement placement, List<BusinessPlan> plans) {
		AffiliateLink link = new AffiliateLink(
				"Ad",
				"Description",
				"https://example.com",
				"https://example.com/image.png",
				"Example",
				"TOOLS",
				placement,
				AffiliateLinkStatus.ACTIVE,
				1);
		link.setDisplayPlans(plans.contains(BusinessPlan.FREE_TRIAL), plans.contains(BusinessPlan.PLUS), plans.contains(BusinessPlan.PRO));
		ReflectionTestUtils.setField(link, "id", 10L);
		return link;
	}

	private TrackerUser workerWithPlan(Long id, BusinessPlan plan) {
		TrackerUser user = new TrackerUser("worker-" + id, "worker" + id + "@example.com", "encoded", new Privilege(PrivilegeRole.Worker));
		ReflectionTestUtils.setField(user, "id", id);
		Business business = new Business("Business " + id, user);
		business.setBusinessPlan(plan);
		user.setBusiness(business);
		return user;
	}
}
