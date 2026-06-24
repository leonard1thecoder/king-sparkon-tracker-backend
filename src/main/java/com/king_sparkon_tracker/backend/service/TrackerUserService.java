package com.king_sparkon_tracker.backend.service;

import java.util.List;
import java.util.Locale;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.CreateWorkerRequest;
import com.king_sparkon_tracker.backend.dto.CompleteOnboardingRequest;
import com.king_sparkon_tracker.backend.dto.LoginRequest;
import com.king_sparkon_tracker.backend.dto.RegisterAdministratorRequest;
import com.king_sparkon_tracker.backend.dto.RegisterAffiliateRequest;
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
	private static final String ADMIN_EMAIL_DOMAIN = "@kingsparkon.com";
	private static final long MAX_ADMINISTRATORS = 1L;

	private final TrackerUserRepository userRepository;
	private final BusinessRepository businessRepository;
	private final PrivilegeService privilegeService;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final EmailVerificationService emailVerificationService;
	private final BusinessPlanPolicyService businessPlanPolicyService;
	private final BusinessAccessService businessAccessService;
	private final AppEmailService appEmailService;
	private final String workerTipUrlTemplate;
	private final String affiliatePromotionUrlTemplate;

	public TrackerUserService(
			TrackerUserRepository userRepository,
			BusinessRepository businessRepository,
			PrivilegeService privilegeService,
			PasswordEncoder passwordEncoder,
			AuditLogService auditLogService,
			EmailVerificationService emailVerificationService,
			BusinessPlanPolicyService businessPlanPolicyService,
			BusinessAccessService businessAccessService,
			AppEmailService appEmailService,
			@Value("${app.tips.worker-tip-url-template:http://localhost:3000/tips/workers/{workerId}}") String workerTipUrlTemplate,
			@Value("${app.affiliates.promotion-url-template:http://localhost:3000/pricing?affiliateCode={affiliateCode}}") String affiliatePromotionUrlTemplate) {
		this.userRepository = userRepository;
		this.emailVerificationService = emailVerificationService;
		this.businessRepository = businessRepository;
		this.privilegeService = privilegeService;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.businessPlanPolicyService = businessPlanPolicyService;
		this.businessAccessService = businessAccessService;
		this.appEmailService = appEmailService;
		this.workerTipUrlTemplate = workerTipUrlTemplate;
		this.affiliatePromotionUrlTemplate = affiliatePromotionUrlTemplate;
	}

	public TrackerUser registerOwner(RegisterUserRequest request) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");
		String businessName = normalizeRequired(request.businessName(), "Business name is required");
		LocalizationCountry localizationCountry = normalizeLocalizationCountry(request.localizationCountry());
		String physicalAddress = normalizeOptional(request.physicalAddress());
		String cellphoneNumber = normalizeOptional(request.cellphoneNumber());

		TrackerUser owner = createUser(
				username,
				emailAddress,
				password,
				PrivilegeRole.Owner,
				null,
				localizationCountry
		);
		if (physicalAddress != null && cellphoneNumber != null) {
			owner.completeOnboarding(physicalAddress, cellphoneNumber);
		}

		Business business = businessRepository.save(new Business(businessName, owner));
		applyAffiliateReferral(business, request.affiliateCode());
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

	public TrackerUser registerAdministrator(RegisterAdministratorRequest request) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");
		LocalizationCountry localizationCountry = normalizeLocalizationCountry(request.localizationCountry());
		String physicalAddress = normalizeRequired(request.physicalAddress(), "Physical address is required");
		String cellphoneNumber = normalizeRequired(request.cellphoneNumber(), "Cellphone number is required");

		requireAdministratorEmailDomain(emailAddress);
		requireAdministratorSlotAvailable();

		TrackerUser administrator = createUser(
				username,
				emailAddress,
				password,
				PrivilegeRole.Admin,
				null,
				localizationCountry
		);
		administrator.completeOnboarding(physicalAddress, cellphoneNumber);

		TrackerUser savedAdministrator = userRepository.save(administrator);

		auditLogService.record(
				"ADMINISTRATOR_REGISTERED",
				"TrackerUser",
				String.valueOf(savedAdministrator.getId()),
				username,
				"Administrator registered with kingsparkon.com domain, localizationCountry: "
						+ savedAdministrator.getLocalizationCountry(),
				null
		);

		emailVerificationService.sendVerificationEmail(savedAdministrator, null, null);

		log.info(
				"administrator_registered userId={} username={} emailDomain={} localizationCountry={} onboardingCompleted={}",
				savedAdministrator.getId(),
				username,
				ADMIN_EMAIL_DOMAIN,
				savedAdministrator.getLocalizationCountry(),
				savedAdministrator.isOnboardingCompleted()
		);

		return savedAdministrator;
	}

	public TrackerUser registerAffiliate(RegisterAffiliateRequest request) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");
		LocalizationCountry localizationCountry = normalizeLocalizationCountry(request.localizationCountry());

		TrackerUser affiliate = createUser(
				username,
				emailAddress,
				password,
				PrivilegeRole.Affiliate,
				null,
				localizationCountry);

		String affiliateCode = newAffiliateCode(username);
		String promotionUrl = affiliatePromotionUrl(affiliateCode);
		affiliate.activateAffiliateProfile(
				affiliateCode,
				promotionUrl,
				qrCodeUrl(promotionUrl));

		if (hasAnyText(request.physicalAddress(), request.cellphoneNumber(), request.paypalLink())) {
			affiliate.completeAffiliateOnboarding(
					normalizeRequired(request.physicalAddress(), "Physical address is required"),
					normalizeRequired(request.cellphoneNumber(), "Cellphone number is required"),
					normalizeRequired(request.paypalLink(), "PayPal link is required"));
		}

		TrackerUser savedAffiliate = userRepository.save(affiliate);

		auditLogService.record(
				"AFFILIATE_REGISTERED",
				"TrackerUser",
				String.valueOf(savedAffiliate.getId()),
				username,
				"Affiliate registered with promotion code: " + savedAffiliate.getAffiliateCode(),
				null);

		emailVerificationService.sendVerificationEmail(savedAffiliate, null, null);

		log.info(
				"affiliate_registered userId={} username={} localizationCountry={} affiliateCode={}",
				savedAffiliate.getId(),
				username,
				savedAffiliate.getLocalizationCountry(),
				savedAffiliate.getAffiliateCode());

		return savedAffiliate;
	}

	public TrackerUser createWorker(CreateWorkerRequest request) {
		throw new IllegalArgumentException("Worker creation requires a business owner");
	}

	public TrackerUser createWorker(CreateWorkerRequest request, String actorUsername) {
		String username = normalizeRequired(request.username(), "Username is required");
		String emailAddress = normalizeEmailAddress(request.emailAddress());
		String password = normalizeRequired(request.password(), "Password is required");
		String jobTitle = normalizeRequired(request.jobTitle(), "Job title is required");
		boolean tipQrCodeEnabled = Boolean.TRUE.equals(request.tipQrCodeEnabled());

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
		worker.updateWorkerProfile(jobTitle, tipQrCodeEnabled);
		if (tipQrCodeEnabled) {
			worker.assignTipQrCodeUrl(workerTipQrCodeUrl(worker));
		}
		worker = userRepository.save(worker);

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

		sendWorkerCreatedNotification(worker, business);

		return worker;
	}

	public TrackerUser completeOnboarding(CompleteOnboardingRequest request, String actorUsername) {
		TrackerUser user = getUserByUsername(actorUsername);
		user.completeOnboarding(
				normalizeRequired(request.physicalAddress(), "Physical address is required"),
				normalizeRequired(request.cellphoneNumber(), "Cellphone number is required"));
		return userRepository.save(user);
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

		PrivilegeRole role = user.getPrivilege().getName();
		if (requiresVerifiedEmail(role) && !user.isEmailVerified()) {
			log.warn("login_failed username={} reason=email_not_verified", username);
			throw new EmailNotVerifiedException();
		}

		log.info(
				"login_succeeded userId={} username={} role={} localizationCountry={}",
				user.getId(),
				username,
				role,
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

	private void requireAdministratorEmailDomain(String emailAddress) {
		if (!emailAddress.endsWith(ADMIN_EMAIL_DOMAIN)) {
			throw new IllegalArgumentException("Administrator email must use the @kingsparkon.com domain");
		}
	}

	private void requireAdministratorSlotAvailable() {
		long administratorCount = userRepository.countByPrivilege_Name(PrivilegeRole.Admin);
		if (administratorCount >= MAX_ADMINISTRATORS) {
			throw new IllegalArgumentException("Administrator account is already registered");
		}
	}

	private boolean requiresVerifiedEmail(PrivilegeRole role) {
		return role == PrivilegeRole.Admin
				|| role == PrivilegeRole.Owner
				|| role == PrivilegeRole.Affiliate;
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

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
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

	private String workerTipQrCodeUrl(TrackerUser worker) {
		String tipUrl = workerTipUrlTemplate.replace("{workerId}", String.valueOf(worker.getId()));
		return qrCodeUrl(tipUrl);
	}

	private String newAffiliateCode(String username) {
		String base = username.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
		if (base.length() > 20) {
			base = base.substring(0, 20);
		}
		if (base.isBlank()) {
			base = "AFFILIATE";
		}

		for (int attempt = 0; attempt < 5; attempt++) {
			String code = "AFF-" + base + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
			if (!userRepository.existsByAffiliateCode(code)) {
				return code;
			}
		}

		throw new IllegalStateException("Could not generate unique affiliate code");
	}

	private String affiliatePromotionUrl(String affiliateCode) {
		return affiliatePromotionUrlTemplate.replace("{affiliateCode}", affiliateCode);
	}

	private String qrCodeUrl(String targetUrl) {
		String encodedUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
		return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=%s".formatted(encodedUrl);
	}

	public TrackerUser affiliateByCode(String affiliateCode) {
		String normalizedCode = normalizeRequired(affiliateCode, "Affiliate code is required").trim().toUpperCase(Locale.ROOT);
		TrackerUser affiliate = userRepository.findByAffiliateCode(normalizedCode)
				.orElseThrow(() -> new ResourceNotFoundException("Affiliate code not found: " + normalizedCode));
		if (affiliate.getPrivilege() == null || affiliate.getPrivilege().getName() != PrivilegeRole.Affiliate) {
			throw new IllegalArgumentException("Referral code does not belong to an affiliate");
		}
		return affiliate;
	}

	public void applyAffiliateReferral(Business business, String affiliateCode) {
		String normalizedCode = normalizeOptional(affiliateCode);
		if (business == null || normalizedCode == null || business.getAffiliate() != null) {
			return;
		}

		TrackerUser affiliate = affiliateByCode(normalizedCode);
		business.assignAffiliate(affiliate, affiliate.getAffiliateCode());
	}

	private LocalizationCountry normalizeLocalizationCountry(LocalizationCountry localizationCountry) {
		return localizationCountry == null ? LocalizationCountry.SOUTH_AFRICA : localizationCountry;
	}

	private boolean hasAnyText(String... values) {
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				return true;
			}
		}
		return false;
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

	private void sendWorkerCreatedNotification(TrackerUser worker, Business business) {
		try {
			appEmailService.sendWorkerCreatedEmail(worker, business);
		} catch (RuntimeException exception) {
			log.warn(
					"worker_created_email_failed_non_blocking recipient={} businessId={} workerId={} reason={}",
					AppEmailService.maskEmail(worker.getEmailAddress()),
					business.getId(),
					worker.getId(),
					exception.getMessage());
		}
	}
}
