package com.king_sparkon_tracker.backend.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.AdminUpdateUserEmailAddressRequest;
import com.king_sparkon_tracker.backend.dto.ChangeEmailAddressRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateEmailAddressException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class UserEmailAddressService {

	private static final Logger log = LoggerFactory.getLogger(UserEmailAddressService.class);

	private final TrackerUserRepository userRepository;
	private final AuditLogService auditLogService;

	public UserEmailAddressService(TrackerUserRepository userRepository, AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.auditLogService = auditLogService;
	}

	public TrackerUser changeOwnEmailAddress(ChangeEmailAddressRequest request, String actorUsername) {
		TrackerUser user = userRepository.findByUsername(normalizeRequired(actorUsername, "Username is required"))
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));
		String nextEmail = normalizeEmailAddress(request.emailAddress());
		updateEmailAddress(user, nextEmail, actorUsername, "USER_EMAIL_CHANGED");
		return user;
	}

	public TrackerUser adminUpdateEmailAddress(Long userId, AdminUpdateUserEmailAddressRequest request, String actorUsername) {
		TrackerUser admin = userRepository.findByUsername(normalizeRequired(actorUsername, "Username is required"))
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));
		requireAdmin(admin);
		TrackerUser user = userRepository.findById(requirePresent(userId, "User id is required"))
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
		String nextEmail = normalizeEmailAddress(request.emailAddress());
		updateEmailAddress(user, nextEmail, actorUsername, "ADMIN_USER_EMAIL_CHANGED");
		return user;
	}

	private void updateEmailAddress(TrackerUser user, String nextEmail, String actorUsername, String auditAction) {
		String currentEmail = user.getEmailAddress();
		if (currentEmail != null && currentEmail.equalsIgnoreCase(nextEmail)) {
			return;
		}
		if (userRepository.existsByEmailAddress(nextEmail)) {
			throw new DuplicateEmailAddressException(nextEmail);
		}
		user.setEmailAddress(nextEmail);
		TrackerUser savedUser = userRepository.save(user);
		auditLogService.record(
				auditAction,
				"TrackerUser",
				String.valueOf(savedUser.getId()),
				actorUsername,
				"Email address updated from " + maskEmail(currentEmail) + " to " + maskEmail(nextEmail),
				savedUser.getBusiness());
		log.info("email_address_updated userId={} actor={} action={}", savedUser.getId(), actorUsername, auditAction);
	}

	private void requireAdmin(TrackerUser user) {
		if (user.getPrivilege() == null || user.getPrivilege().getName() != PrivilegeRole.Admin) {
			throw new IllegalArgumentException("Admin role is required");
		}
	}

	private String normalizeEmailAddress(String emailAddress) {
		return normalizeRequired(emailAddress, "Email address is required").toLowerCase(Locale.ROOT);
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private <T> T requirePresent(T value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

	private String maskEmail(String email) {
		if (!StringUtils.hasText(email)) {
			return "***";
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}
}
