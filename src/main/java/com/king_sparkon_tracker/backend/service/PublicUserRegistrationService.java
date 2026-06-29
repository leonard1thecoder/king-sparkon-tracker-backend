package com.king_sparkon_tracker.backend.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateEmailAddressException;
import com.king_sparkon_tracker.backend.exception.DuplicateUsernameException;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class PublicUserRegistrationService {

	private static final Logger log = LoggerFactory.getLogger(PublicUserRegistrationService.class);

	private final TrackerUserRepository userRepository;
	private final PrivilegeService privilegeService;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final EmailVerificationService emailVerificationService;

	public PublicUserRegistrationService(
			TrackerUserRepository userRepository,
			PrivilegeService privilegeService,
			PasswordEncoder passwordEncoder,
			AuditLogService auditLogService,
			EmailVerificationService emailVerificationService) {
		this.userRepository = userRepository;
		this.privilegeService = privilegeService;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.emailVerificationService = emailVerificationService;
	}

	public TrackerUser registerUser(RegisterUserRequest request) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");
		LocalizationCountry localizationCountry = request.localizationCountry() == null
				? LocalizationCountry.SOUTH_AFRICA
				: request.localizationCountry();

		if (userRepository.existsByUsername(username)) {
			throw new DuplicateUsernameException(username);
		}

		if (userRepository.existsByEmailAddress(emailAddress)) {
			throw new DuplicateEmailAddressException(emailAddress);
		}

		Privilege privilege = privilegeService.createPrivilege(PrivilegeRole.User);
		TrackerUser user = new TrackerUser(username, emailAddress, passwordEncoder.encode(password), privilege, localizationCountry);

		String physicalAddress = normalizeOptional(request.physicalAddress());
		String cellphoneNumber = normalizeOptional(request.cellphoneNumber());
		if (physicalAddress != null && cellphoneNumber != null) {
			user.completeOnboarding(physicalAddress, cellphoneNumber);
		}

		TrackerUser savedUser = userRepository.save(user);

		auditLogService.record(
				"USER_REGISTERED",
				"TrackerUser",
				String.valueOf(savedUser.getId()),
				username,
				"Public user registered with localizationCountry: " + savedUser.getLocalizationCountry(),
				null);

		emailVerificationService.sendVerificationEmail(savedUser, null, null);

		log.info(
				"public_user_registered userId={} username={} localizationCountry={}",
				savedUser.getId(),
				username,
				savedUser.getLocalizationCountry());

		return savedUser;
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

	private String normalizeEmailAddress(String emailAddress) {
		return normalizeRequired(emailAddress, "Email address is required").toLowerCase(Locale.ROOT);
	}
}
