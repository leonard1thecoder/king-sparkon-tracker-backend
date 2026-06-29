package com.king_sparkon_tracker.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.AffiliateOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.AffiliateProfileResponse;
import com.king_sparkon_tracker.backend.dto.CompleteOnboardingRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class OnboardingProfileService {

	private final TrackerUserRepository userRepository;

	@Autowired(required = false)
	private GoogleStorageService googleStorageService;

	public OnboardingProfileService(TrackerUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public TrackerUser completeUserOnboarding(CompleteOnboardingRequest request, String username) {
		TrackerUser user = user(username);
		user.completeOnboarding(
				normalizeRequired(request.physicalAddress(), "Physical address is required"),
				normalizeRequired(request.cellphoneNumber(), "Cellphone number is required"),
				storeProfilePicture(request.profilePictureUrl(), "profiles/users", username));
		return userRepository.save(user);
	}

	public AffiliateProfileResponse completeAffiliateOnboarding(AffiliateOnboardingRequest request, String username) {
		TrackerUser affiliate = user(username);
		if (affiliate.getPrivilege() == null || affiliate.getPrivilege().getName() != PrivilegeRole.Affiliate) {
			throw new IllegalArgumentException("Only affiliate accounts can complete affiliate onboarding");
		}
		affiliate.completeAffiliateOnboarding(
				normalizeRequired(request.physicalAddress(), "Physical address is required"),
				normalizeRequired(request.cellphoneNumber(), "Cellphone number is required"),
				normalizeRequired(request.paypalLink(), "PayPal link is required"),
				storeProfilePicture(request.profilePictureUrl(), "profiles/affiliates", username));
		return AffiliateProfileResponse.from(userRepository.save(affiliate));
	}

	private TrackerUser user(String username) {
		String normalizedUsername = normalizeRequired(username, "Username is required");
		return userRepository.findByUsername(normalizedUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedUsername));
	}

	private String storeProfilePicture(String value, String folder, String ownerKey) {
		if (googleStorageService == null) {
			return normalizeOptional(value);
		}
		return googleStorageService.storeImageValue(value, folder, ownerKey);
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
