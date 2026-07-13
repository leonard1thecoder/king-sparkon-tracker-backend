package com.king_sparkon_tracker.backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.UpdateCurrentUserPasswordRequest;
import com.king_sparkon_tracker.backend.dto.UpdateCurrentUserProfileRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateEmailAddressException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class CurrentUserProfileService {

	private final TrackerUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;

	public CurrentUserProfileService(
			TrackerUserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
	}

	public TrackerUser updateProfile(UpdateCurrentUserProfileRequest request, String actorUsername) {
		TrackerUser user = currentUser(actorUsername);
		String emailAddress = required(request.emailAddress(), "Email address is required").toLowerCase(Locale.ROOT);
		String physicalAddress = required(request.physicalAddress(), "Physical address is required");
		String cellphoneNumber = required(request.cellphoneNumber(), "Cellphone number is required");
		String profilePictureUrl = optional(request.profilePictureUrl());

		if (!emailAddress.equalsIgnoreCase(user.getEmailAddress())) {
			if (userRepository.existsByEmailAddress(emailAddress)) {
				throw new DuplicateEmailAddressException(emailAddress);
			}
			user.setEmailAddress(emailAddress);
			user.updateEmailVerificationStatus(false);
		}

		user.completeOnboarding(physicalAddress, cellphoneNumber, profilePictureUrl);
		TrackerUser saved = userRepository.save(user);
		auditLogService.record(
				"CURRENT_USER_PROFILE_UPDATED",
				"TrackerUser",
				String.valueOf(saved.getId()),
				actorUsername,
				"User updated email, address, cellphone and profile picture",
				saved.getBusiness());
		return saved;
	}

	public void updatePassword(UpdateCurrentUserPasswordRequest request, String actorUsername) {
		TrackerUser user = currentUser(actorUsername);
		String currentPassword = required(request.currentPassword(), "Current password is required");
		String newPassword = required(request.newPassword(), "New password is required");
		String confirmPassword = required(request.confirmPassword(), "Password confirmation is required");

		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new IllegalArgumentException("Current password is incorrect");
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("New password and confirmation must match");
		}
		if (passwordEncoder.matches(newPassword, user.getPassword())) {
			throw new IllegalArgumentException("New password must be different from the current password");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		auditLogService.record(
				"CURRENT_USER_PASSWORD_UPDATED",
				"TrackerUser",
				String.valueOf(user.getId()),
				actorUsername,
				"User updated account password",
				user.getBusiness());
	}

	private TrackerUser currentUser(String actorUsername) {
		String username = required(actorUsername, "Authenticated username is required");
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String optional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
