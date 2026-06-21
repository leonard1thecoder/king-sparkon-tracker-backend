package com.king_sparkon_tracker.backend.service;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.LoginRequest;
import com.king_sparkon_tracker.backend.dto.RegisterUserRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateEmailAddressException;
import com.king_sparkon_tracker.backend.exception.DuplicateUsernameException;
import com.king_sparkon_tracker.backend.exception.EmailNotVerifiedException;
import com.king_sparkon_tracker.backend.exception.InvalidCredentialsException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;

@Service
@Transactional
public class TrackerUserService {

	private static final Logger log = LoggerFactory.getLogger(TrackerUserService.class);

	private final TrackerUserRepository userRepository;
	private final BusinessRepository businessRepository;
	private final PrivilegeService privilegeService;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final EmailVerificationService emailVerificationService;
	private final BusinessPlanPolicyService businessPlanPolicyService;
	private final BusinessAccessService businessAccessService;

	public TrackerUserService(
			TrackerUserRepository userRepository,
			BusinessRepository businessRepository,
			PrivilegeService privilegeService,
			PasswordEncoder passwordEncoder,
			AuditLogService auditLogService,
			EmailVerificationService emailVerificationService,
			BusinessPlanPolicyService businessPlanPolicyService,
			BusinessAccessService businessAccessService) {
		this.userRepository = userRepository;
		this.emailVerificationService = emailVerificationService;
		this.businessRepository = businessRepository;
		this.privilegeService = privilegeService;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.businessPlanPolicyService = businessPlanPolicyService;
		this.businessAccessService = businessAccessService;
	}

	public TrackerUser registerOwner(RegisterUserRequest request) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");
		String businessName = normalizeRequired(request.businessName(), "Business name is required");
		LocalizationCountry localizationCountry = normalizeLocalizationCountry(request.localizationCountry());

		TrackerUser owner = createUser(
				username,
				emailAddress,
				password,
				PrivilegeRole.Owner,
				null,
				localizationCountry
		);

		Business business = businessRepository.save(new Business(businessName, owner));
		owner.setBusiness(business);

		TrackerUser savedOwner = userRepository.save(owner);

		auditLogService.record(
				"OWNER_REGISTERED",
				"TrackerUser",
				String.valueOf(savedOwner.getId()),
				username,
				"Owner registered business: " + business.getName()
						+ ", localizationCountry: " + savedOwner.getLocalizationCountry()
						+ ", businessPlan: " + business.getBusinessPlan()
						+ ", businessStatus: " + business.getBusinessStatus(),
				business
		);

		emailVerificationService.sendVerificationEmail(savedOwner, null, null);

		log.info(
				"owner_registered userId={} username={} businessId={} localizationCountry={} businessPlan={} businessStatus={}",
				savedOwner.getId(),
				username,
				business.getId(),
				savedOwner.getLocalizationCountry(),
				business.getBusinessPlan(),
				business.getBusinessStatus()
		);

		return savedOwner;
	}

	public TrackerUser createWorker(CreateWorkerRequest request) {
		throw new IllegalArgumentException("Worker creation requires a business owner");
	}

	public TrackerUser createWorker(CreateWorkerRequest request, String actorUsername) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");

		TrackerUser owner = getUserByUsername(actorUsername);
		requireRole(owner, PrivilegeRole.Owner, "Only business owners can create workers");

		Business business = requireBusiness(owner);
		businessAccessService.requireFeature(actorUsername, BusinessFeature.CREATE_WORKERS);
		requireWorkerLimitAvailable(business, actorUsername);

		TrackerUser worker = createUser(
				username,
				emailAddress,
				password,
				PrivilegeRole.Worker,
				business,
				owner.getLocalizationCountry()
		);

		auditLogService.record(
				"WORKER_CREATED",
				"TrackerUser",
				String.valueOf(worker.getId()),
				actorUsername,
				"Worker created: " + worker.getUsername()
						+ ", localizationCountry: " + worker.getLocalizationCountry()
						+ ", businessPlan: " + business.getBusinessPlan()
						+ ", workerLimit: " + workerLimitLabel(businessPlanPolicyService.maxWorkers(business)),
				business
		);

		log.info(
				"worker_created userId={} username={} businessId={} localizationCountry={} businessPlan={} workerLimit={} actor={}",
				worker.getId(),
				worker.getUsername(),
				business.getId(),
				worker.getLocalizationCountry(),
				business.getBusinessPlan(),
				workerLimitLabel(businessPlanPolicyService.maxWorkers(business)),
				actorUsername
		);

		return worker;
	}

	@Transactional(readOnly = true)
	public TrackerUser authenticate(LoginRequest request) {
		String username = normalizeRequired(request.username(), "Username is required");
		String password = normalizeRequired(request.password(), "Password is required");

		TrackerUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> {
					log.warn("login_failed username={} reason=user_not_found", username);
					return new InvalidCredentialsException();
				});

		if (!passwordEncoder.matches(password, user.getPassword())) {
			log.warn("login_failed username={} reason=password_mismatch", username);
			throw new InvalidCredentialsException();
		}

		if (user.getPrivilege().getName() == PrivilegeRole.Owner && !user.isEmailVerified()) {
			log.warn("login_failed username={} reason=email_not_verified", username);
			throw new EmailNotVerifiedException();
		}

		log.info(
				"login_succeeded userId={} username={} role={} localizationCountry={}",
				user.getId(),
				username,
				user.getPrivilege().getName(),
				user.getLocalizationCountry()
		);

		return user;
	}

	@Transactional(readOnly = true)
	public List<TrackerUser> listUsers() {
		log.debug("users_list_requested mode=all");
		return userRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Page<TrackerUser> listUsers(Pageable pageable) {
		log.debug("users_list_requested page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
		return userRepository.findAll(pageable);
	}

	public void deleteWorker(Long workerId, String actorUsername) {
		if (workerId == null) {
			throw new IllegalArgumentException("Worker id is required");
		}

		TrackerUser owner = getUserByUsername(actorUsername);
		requireRole(owner, PrivilegeRole.Owner, "Only business owners can delete workers");

		Business business = requireBusiness(owner);

		TrackerUser worker = userRepository.findByIdAndBusiness_Id(workerId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));

		if (worker.getId().equals(owner.getId())) {
			log.warn("worker_delete_rejected reason=owner_self_delete workerId={} actor={}", workerId, actorUsername);
			throw new IllegalArgumentException("Owner cannot be deleted through worker delete endpoint");
		}

		if (worker.getPrivilege().getName() != PrivilegeRole.Worker) {
			throw new IllegalArgumentException("Only worker accounts can be deleted");
		}

		auditLogService.record(
				"WORKER_DELETED",
				"TrackerUser",
				String.valueOf(worker.getId()),
				actorUsername,
				"Worker deleted: " + worker.getUsername(),
				business
		);

		userRepository.delete(worker);
	}

	@Transactional(readOnly = true)
	public Page<TrackerUser> listUsers(Pageable pageable, String actorUsername) {
		Business business = businessForActor(actorUsername);
		return userRepository.findByBusiness_Id(business.getId(), pageable);
	}

	@Transactional(readOnly = true)
	public TrackerUser getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	@Transactional(readOnly = true)
	public TrackerUser getUserById(Long id, String actorUsername) {
		Business business = businessForActor(actorUsername);
		return userRepository.findByIdAndBusiness_Id(id, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	@Transactional(readOnly = true)
	public TrackerUser getUserByUsername(String username) {
		String normalizedUsername = normalizeRequired(username, "Username is required");
		return userRepository.findByUsername(normalizedUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedUsername));
	}

	@Transactional(readOnly = true)
	public Business businessForActor(String actorUsername) {
		return requireBusiness(getUserByUsername(actorUsername));
	}

	private void requireWorkerLimitAvailable(Business business, String actorUsername) {
		int workerLimit = businessPlanPolicyService.maxWorkers(business);

		if (workerLimit == BusinessPlanPolicyService.UNLIMITED) {
			return;
		}

		long currentWorkers = userRepository.countByBusiness_IdAndPrivilege_Name(
				business.getId(),
				PrivilegeRole.Worker
		);

		if (currentWorkers >= workerLimit) {
			log.warn(
					"worker_create_rejected reason=worker_limit_reached businessId={} plan={} workerLimit={} currentWorkers={} actor={}",
					business.getId(),
					business.getBusinessPlan(),
					workerLimit,
					currentWorkers,
					actorUsername
			);

			throw new IllegalArgumentException(
					"Worker limit reached for " + business.getBusinessPlan()
							+ " plan. Current limit: " + workerLimit
			);
		}
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}

		return value.trim();
	}

	private TrackerUser createUser(
			String username,
			String emailAddress,
			String password,
			PrivilegeRole role,
			Business business) {
		return createUser(username, emailAddress, password, role, business, LocalizationCountry.SOUTH_AFRICA);
	}

	private TrackerUser createUser(
			String username,
			String emailAddress,
			String password,
			PrivilegeRole role,
			Business business,
			LocalizationCountry localizationCountry) {
		if (userRepository.existsByUsername(username)) {
			throw new DuplicateUsernameException(username);
		}

		if (userRepository.existsByEmailAddress(emailAddress)) {
			throw new DuplicateEmailAddressException(emailAddress);
		}

		Privilege privilege = privilegeService.createPrivilege(role);

		TrackerUser user = new TrackerUser(
				username,
				emailAddress,
				passwordEncoder.encode(password),
				privilege,
				normalizeLocalizationCountry(localizationCountry)
		);

		user.setBusiness(business);

		return userRepository.save(user);
	}

	private String normalizeEmailAddress(String emailAddress) {
		return normalizeRequired(emailAddress, "Email address is required").toLowerCase(Locale.ROOT);
	}

	private LocalizationCountry normalizeLocalizationCountry(LocalizationCountry localizationCountry) {
		return localizationCountry == null ? LocalizationCountry.SOUTH_AFRICA : localizationCountry;
	}

	private Business requireBusiness(TrackerUser user) {
		if (user.getBusiness() == null) {
			throw new IllegalArgumentException("User is not linked to a business");
		}

		return user.getBusiness();
	}

	private void requireRole(TrackerUser user, PrivilegeRole role, String message) {
		if (user.getPrivilege().getName() != role) {
			throw new IllegalArgumentException(message);
		}
	}

	private String workerLimitLabel(int workerLimit) {
		return workerLimit == BusinessPlanPolicyService.UNLIMITED ? "UNLIMITED" : String.valueOf(workerLimit);
	}
}
