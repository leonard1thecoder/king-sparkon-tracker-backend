package com.king_sparkon_tracker.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.service.EmailVerificationService;
import com.king_sparkon_tracker.backend.service.PasswordResetService;
import com.king_sparkon_tracker.backend.service.RefreshTokenService;
import com.king_sparkon_tracker.backend.service.TrackerUserService;

class OwnerBusinessDescriptionRegistrationTest {

	private TrackerUserService userService;
	private BusinessRepository businessRepository;
	private AuthenticationController controller;

	@BeforeEach
	void setUp() {
		userService = mock(TrackerUserService.class);
		businessRepository = mock(BusinessRepository.class);
		controller = new AuthenticationController(
				userService,
				mock(RefreshTokenService.class),
				mock(PasswordResetService.class),
				mock(EmailVerificationService.class));
		ReflectionTestUtils.setField(controller, "businessRepository", businessRepository);
	}

	@Test
	void positivePersistsTrimmedBusinessDescription() {
		RegisterUserRequest request = request("  Barcode retail store with tips.  ");
		TrackerUser owner = ownerWithBusiness();
		when(userService.registerOwner(request)).thenReturn(owner);
		when(businessRepository.save(owner.getBusiness())).thenReturn(owner.getBusiness());

		controller.register(request);

		assertThat(owner.getBusiness().getDescription()).isEqualTo("Barcode retail store with tips.");
		verify(businessRepository).save(owner.getBusiness());
	}

	@Test
	void negativeDoesNotPersistBlankBusinessDescription() {
		RegisterUserRequest request = request("   ");
		TrackerUser owner = ownerWithBusiness();
		when(userService.registerOwner(request)).thenReturn(owner);

		controller.register(request);

		assertThat(owner.getBusiness().getDescription()).isNull();
		verify(businessRepository, never()).save(owner.getBusiness());
	}

	private RegisterUserRequest request(String businessDescription) {
		return new RegisterUserRequest(
				"owner",
				"owner@example.com",
				"secret",
				"Spark Store",
				businessDescription,
				LocalizationCountry.SOUTH_AFRICA,
				"12 Main Road",
				"+27821234567",
				null);
	}

	private TrackerUser ownerWithBusiness() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner),
				LocalizationCountry.SOUTH_AFRICA);
		Business business = new Business("Spark Store", owner);
		owner.setBusiness(business);
		return owner;
	}
}
