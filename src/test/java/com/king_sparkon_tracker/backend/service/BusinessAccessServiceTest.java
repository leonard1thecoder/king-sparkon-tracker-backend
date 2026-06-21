package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.BusinessStatus;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class BusinessAccessServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	private BusinessAccessService service;

	@BeforeEach
	void setUp() {
		service = new BusinessAccessService(userRepository, new BusinessPlanPolicyService());
	}

	@Test
	void businessForActorReturnsUsersBusiness() {
		TrackerUser worker = userWithBusiness("worker", BusinessPlan.PLUS, BusinessStatus.ACTIVE);
		when(userRepository.findByUsername("worker")).thenReturn(Optional.of(worker));

		Business business = service.businessForActor("worker");

		assertThat(business).isSameAs(worker.getBusiness());
	}

	@Test
	void businessForActorThrowsWhenUserIsMissing() {
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.businessForActor("missing"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found: missing");
	}

	@Test
	void requireActiveBusinessAllowsTrialBusinesses() {
		when(userRepository.findByUsername("owner"))
				.thenReturn(Optional.of(userWithBusiness("owner", BusinessPlan.FREE_TRIAL, BusinessStatus.TRIAL)));

		service.requireActiveBusiness("owner");

		verify(userRepository).findByUsername("owner");
	}

	@Test
	void requireActiveBusinessRejectsDeactivatedBusinessWithActionableMessage() {
		when(userRepository.findByUsername("owner"))
				.thenReturn(Optional.of(userWithBusiness("owner", BusinessPlan.PLUS, BusinessStatus.DEACTIVATED)));

		assertThatThrownBy(() -> service.requireActiveBusiness("owner"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Business is deactivated. Please activate your subscription");
	}

	@Test
	void requireFeatureRejectsUnavailablePlanFeature() {
		when(userRepository.findByUsername("owner"))
				.thenReturn(Optional.of(userWithBusiness("owner", BusinessPlan.PLUS, BusinessStatus.ACTIVE)));

		assertThatThrownBy(() -> service.requireFeature("owner", BusinessFeature.BUSINESS_ANALYSIS_AI))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Feature BUSINESS_ANALYSIS_AI is not available for this business plan");
	}

	private TrackerUser userWithBusiness(String username, BusinessPlan plan, BusinessStatus status) {
		TrackerUser user = new TrackerUser(
				username,
				username + "@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business(username + " Store", user);
		ReflectionTestUtils.setField(business, "id", 1L);
		business.setBusinessPlan(plan);
		business.setBusinessStatus(status);
		user.setBusiness(business);
		return user;
	}
}
