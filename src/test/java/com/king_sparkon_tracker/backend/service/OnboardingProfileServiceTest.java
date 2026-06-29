package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.dto.AffiliateOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.CompleteOnboardingRequest;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@ExtendWith(MockitoExtension.class)
class OnboardingProfileServiceTest {

	@Mock
	private TrackerUserRepository userRepository;

	private OnboardingProfileService service;

	@BeforeEach
	void setUp() {
		service = new OnboardingProfileService(userRepository);
	}

	@Test
	void completeUserOnboardingStoresProfilePicture() {
		TrackerUser user = new TrackerUser("worker", "worker@example.com", "secret", new Privilege(PrivilegeRole.Worker));
		when(userRepository.findByUsername("worker")).thenReturn(Optional.of(user));
		when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TrackerUser saved = service.completeUserOnboarding(
				new CompleteOnboardingRequest("12 Main Road", "+27821234567", "https://cdn.example.com/worker.png"),
				"worker");

		assertThat(saved.isOnboardingCompleted()).isTrue();
		assertThat(saved.getProfilePictureUrl()).isEqualTo("https://cdn.example.com/worker.png");
	}

	@Test
	void completeAffiliateOnboardingStoresProfilePictureAndPayoutLink() {
		TrackerUser affiliate = new TrackerUser("affiliate", "affiliate@example.com", "secret", new Privilege(PrivilegeRole.Affiliate));
		when(userRepository.findByUsername("affiliate")).thenReturn(Optional.of(affiliate));
		when(userRepository.save(any(TrackerUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.completeAffiliateOnboarding(
				new AffiliateOnboardingRequest("12 Main Road", "+27821234567", "https://payments.example.com/affiliate", "https://cdn.example.com/affiliate.png"),
				"affiliate");

		assertThat(response.onboardingCompleted()).isTrue();
		assertThat(response.profilePictureUrl()).isEqualTo("https://cdn.example.com/affiliate.png");
		assertThat(response.paypalLink()).isEqualTo("https://payments.example.com/affiliate");
	}
}
