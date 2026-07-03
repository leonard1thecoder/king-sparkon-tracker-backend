package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.UpdateUserEmailVerificationStatusRequest;
import com.king_sparkon_tracker.backend.dto.UserEmailVerificationStatusResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class UserEmailVerificationStatusService {

	private static final Logger log = LoggerFactory.getLogger(UserEmailVerificationStatusService.class);

	private final TrackerUserRepository userRepository;
	private final AuditLogService auditLogService;

	public UserEmailVerificationStatusService(TrackerUserRepository userRepository, AuditLogService auditLogService) {
		this.userRepository = userRepository;
		this.auditLogService = auditLogService;
	}

	public UserEmailVerificationStatusResponse updateEmailVerificationStatus(
			Long userId,
			UpdateUserEmailVerificationStatusRequest request,
			String actorUsername) {
		String actor = normalizeRequired(actorUsername, "Username is required");
		TrackerUser admin = userRepository.findByUsername(actor)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor));
		requireAdmin(admin);

		TrackerUser user = userRepository.findById(requirePresent(userId, "User id is required"))
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

		boolean previousStatus = user.isEmailVerified();
		boolean nextStatus = Boolean.TRUE.equals(request.emailVerified());
		user.updateEmailVerificationStatus(nextStatus);
		TrackerUser savedUser = userRepository.save(user);

		auditLogService.record(
				nextStatus ? "USER_EMAIL_VERIFICATION_BYPASSED" : "USER_EMAIL_VERIFICATION_RESET",
				"TrackerUser",
				String.valueOf(savedUser.getId()),
				actor,
				"Email verification status updated from " + previousStatus + " to " + savedUser.isEmailVerified(),
				savedUser.getBusiness());
		log.info(
				"user_email_verification_status_updated userId={} actor={} previousStatus={} nextStatus={}",
				savedUser.getId(),
				actor,
				previousStatus,
				savedUser.isEmailVerified());

		return UserEmailVerificationStatusResponse.from(savedUser, actor);
	}

	private void requireAdmin(TrackerUser user) {
		if (user.getPrivilege() == null || user.getPrivilege().getName() != PrivilegeRole.Admin) {
			throw new IllegalArgumentException("Admin role is required");
		}
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
}
