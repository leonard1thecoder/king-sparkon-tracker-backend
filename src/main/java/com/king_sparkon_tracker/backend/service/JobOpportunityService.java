package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.ApplyJobRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.BookInterviewRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.CreateJobPostRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobApplicationResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobInterviewResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobPostResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobSeekerProfileResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.OpportunitiesResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.UpsertJobSeekerProfileRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.JobApplication;
import com.king_sparkon_tracker.backend.model.JobInterview;
import com.king_sparkon_tracker.backend.model.JobInterviewStatus;
import com.king_sparkon_tracker.backend.model.JobPost;
import com.king_sparkon_tracker.backend.model.JobPostStatus;
import com.king_sparkon_tracker.backend.model.JobSeekerProfile;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.JobApplicationRepository;
import com.king_sparkon_tracker.backend.repository.JobPostRepository;
import com.king_sparkon_tracker.backend.repository.JobSeekerProfileRepository;
import com.king_sparkon_tracker.backend.repository.OpportunityInterviewRepository;

@Service
@Transactional
public class JobOpportunityService {

	private static final Logger log = LoggerFactory.getLogger(JobOpportunityService.class);
	private static final int MAX_INTERESTS = 5;
	private static final int MONEY_SCALE = 2;

	private final JobSeekerProfileRepository profileRepository;
	private final JobPostRepository jobPostRepository;
	private final JobApplicationRepository applicationRepository;
	private final OpportunityInterviewRepository interviewRepository;
	private final TrackerUserService trackerUserService;
	private final AuditLogService auditLogService;
	private final JobOpportunityEmailService emailService;

	public JobOpportunityService(
			JobSeekerProfileRepository profileRepository,
			JobPostRepository jobPostRepository,
			JobApplicationRepository applicationRepository,
			OpportunityInterviewRepository interviewRepository,
			TrackerUserService trackerUserService,
			AuditLogService auditLogService,
			JobOpportunityEmailService emailService) {
		this.profileRepository = profileRepository;
		this.jobPostRepository = jobPostRepository;
		this.applicationRepository = applicationRepository;
		this.interviewRepository = interviewRepository;
		this.trackerUserService = trackerUserService;
		this.auditLogService = auditLogService;
		this.emailService = emailService;
	}

	@Transactional(readOnly = true)
	public JobSeekerProfileResponse profile(String actorUsername) {
		TrackerUser user = trackerUserService.getUserByUsername(actorUsername);
		JobSeekerProfile profile = profileRepository.findByUser_Id(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Job seeker profile not found for: " + actorUsername));
		return JobSeekerProfileResponse.from(profile);
	}

	public JobSeekerProfileResponse upsertProfile(UpsertJobSeekerProfileRequest request, String actorUsername) {
		TrackerUser user = trackerUserService.getUserByUsername(actorUsername);
		List<String> interests = normalizeInterests(request.interestedJobs());
		JobSeekerProfile profile = profileRepository.findByUser_Id(user.getId())
				.orElseGet(() -> new JobSeekerProfile(user, request.highestQualification(), interests, request.experience(), normalizeRequired(request.about(), "About is required")));
		profile.updateProfile(
				request.highestQualification(),
				interests,
				request.experience(),
				normalizeRequired(request.about(), "About is required"));
		JobSeekerProfile saved = profileRepository.save(profile);
		auditLogService.record("JOB_PROFILE_UPSERTED", "JobSeekerProfile", String.valueOf(saved.getId()), actorUsername, "Job opportunity profile saved");
		return JobSeekerProfileResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<JobPostResponse> openJobPosts() {
		return jobPostRepository.findByStatusAndClosingDateGreaterThanEqualOrderByCreatedDateDesc(JobPostStatus.OPEN, LocalDate.now())
				.stream()
				.map(JobPostResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public JobPostResponse jobPost(Long jobPostId) {
		return JobPostResponse.from(requireJobPost(jobPostId));
	}

	@Transactional(readOnly = true)
	public OpportunitiesResponse opportunities(String actorUsername) {
		TrackerUser user = trackerUserService.getUserByUsername(actorUsername);
		return new OpportunitiesResponse(
				openJobPosts(),
				applicationRepository.findByApplicant_IdOrderByCreatedDateDesc(user.getId()).stream().map(JobApplicationResponse::from).toList(),
				interviewsForApplicant(user.getId()).stream().map(JobInterviewResponse::from).toList());
	}

	public JobPostResponse createJobPost(CreateJobPostRequest request, String actorUsername) {
		TrackerUser owner = trackerUserService.getUserByUsername(actorUsername);
		requireOwner(owner);
		Business business = trackerUserService.businessForActor(actorUsername);
		requireDateWindow(request.startingDate(), request.closingDate());

		JobPost post = new JobPost(
				business,
				owner,
				normalizeRequired(request.title(), "Job title is required"),
				request.startingDate(),
				request.closingDate(),
				normalizeRequired(request.jobDescription(), "Job description is required"),
				request.yearsOfExperienceRequired(),
				normalizeOptional(request.jobPostFileUrl()),
				normalizeMoney(request.estimatedSalary()),
				currencyFor(owner));
		JobPost saved = jobPostRepository.save(post);
		auditLogService.record("JOB_POST_CREATED", "JobPost", String.valueOf(saved.getId()), actorUsername, "Job post created: " + saved.getTitle(), business);
		return JobPostResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<JobPostResponse> ownerJobPosts(String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return jobPostRepository.findByBusiness_IdOrderByCreatedDateDesc(business.getId()).stream().map(JobPostResponse::from).toList();
	}

	public JobApplicationResponse apply(Long jobPostId, ApplyJobRequest request, String actorUsername) {
		TrackerUser applicant = trackerUserService.getUserByUsername(actorUsername);
		JobSeekerProfile profile = profileRepository.findByUser_Id(applicant.getId())
				.orElseThrow(() -> new IllegalArgumentException("Create your opportunity profile before applying for jobs"));
		JobPost post = requireJobPost(jobPostId);
		if (!post.isOpen(LocalDate.now())) {
			throw new IllegalArgumentException("Job post is closed");
		}
		if (applicationRepository.findByJobPost_IdAndApplicant_Id(post.getId(), applicant.getId()).isPresent()) {
			throw new IllegalArgumentException("You already applied for this job post");
		}

		JobApplication application = new JobApplication(
				post,
				applicant,
				profile,
				normalizeRequired(request.resumeUrl(), "Resume URL is required"),
				normalizeOptionalList(request.certificateUrls()));
		JobApplication saved = applicationRepository.save(application);
		auditLogService.record("JOB_APPLICATION_SUBMITTED", "JobApplication", String.valueOf(saved.getId()), actorUsername, "Applied for job post: " + post.getTitle(), post.getBusiness());
		sendApplicationSubmittedEmails(saved);
		return JobApplicationResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<JobApplicationResponse> myApplications(String actorUsername) {
		TrackerUser user = trackerUserService.getUserByUsername(actorUsername);
		return applicationRepository.findByApplicant_IdOrderByCreatedDateDesc(user.getId()).stream().map(JobApplicationResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<JobApplicationResponse> ownerApplications(String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return applicationRepository.findByJobPost_Business_IdOrderByCreatedDateDesc(business.getId()).stream().map(JobApplicationResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<JobApplicationResponse> ownerApplicationsForJob(Long jobPostId, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		JobPost post = jobPostRepository.findByIdAndBusiness_Id(jobPostId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Job post not found: " + jobPostId));
		return applicationRepository.findByJobPost_IdOrderByCreatedDateDesc(post.getId()).stream().map(JobApplicationResponse::from).toList();
	}

	public JobApplicationResponse viewApplication(Long applicationId, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		JobApplication application = ownerApplication(applicationId, business.getId());
		application.markViewed();
		JobApplication saved = applicationRepository.save(application);
		auditLogService.record("JOB_APPLICATION_VIEWED", "JobApplication", String.valueOf(saved.getId()), actorUsername, "Application viewed", business);
		return JobApplicationResponse.from(saved);
	}

	public JobApplicationResponse declineApplication(Long applicationId, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		JobApplication application = ownerApplication(applicationId, business.getId());
		application.reject();
		JobApplication saved = applicationRepository.save(application);
		auditLogService.record("JOB_APPLICATION_DECLINED", "JobApplication", String.valueOf(saved.getId()), actorUsername, "Application declined", business);
		sendDeclinedEmail(saved);
		return JobApplicationResponse.from(saved);
	}

	public JobInterviewResponse bookInterview(Long applicationId, BookInterviewRequest request, String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		JobApplication application = ownerApplication(applicationId, business.getId());
		if (interviewRepository.findAll().stream().anyMatch(interview -> interview.getApplication().getId().equals(applicationId))) {
			throw new IllegalArgumentException("Interview already booked for this application");
		}
		if (!request.interviewExpiresAt().isBefore(request.interviewDate())) {
			throw new IllegalArgumentException("Interview expiry must be before the interview date");
		}

		application.markAccepted();
		application.markInterviewBooked();
		applicationRepository.save(application);

		JobInterview interview = new JobInterview(
				application,
				request.interviewDate(),
				normalizeRequired(request.interviewDescription(), "Interview description is required"),
				request.interviewExpiresAt(),
				businessAddress(business));
		JobInterview saved = interviewRepository.save(interview);
		auditLogService.record("JOB_INTERVIEW_BOOKED", "JobInterview", String.valueOf(saved.getId()), actorUsername, "Interview booked for application: " + application.getId(), business);
		sendInterviewBookedEmail(saved);
		return JobInterviewResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public List<JobInterviewResponse> myInterviews(String actorUsername) {
		TrackerUser user = trackerUserService.getUserByUsername(actorUsername);
		return interviewsForApplicant(user.getId()).stream().map(JobInterviewResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<JobInterviewResponse> ownerInterviews(String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return interviewsForBusiness(business.getId()).stream().map(JobInterviewResponse::from).toList();
	}

	public JobInterviewResponse acceptInterview(Long interviewId, String actorUsername) {
		TrackerUser applicant = trackerUserService.getUserByUsername(actorUsername);
		JobInterview interview = interviewForApplicant(interviewId, applicant.getId());
		requireInterviewActionAllowed(interview);
		interview.accept();
		JobInterview saved = interviewRepository.save(interview);
		auditLogService.record("JOB_INTERVIEW_ACCEPTED", "JobInterview", String.valueOf(saved.getId()), actorUsername, "Interview accepted", saved.getBusiness());
		sendInterviewAcceptedOwnerEmail(saved);
		return JobInterviewResponse.from(saved);
	}

	public JobInterviewResponse declineInterview(Long interviewId, String actorUsername) {
		TrackerUser applicant = trackerUserService.getUserByUsername(actorUsername);
		JobInterview interview = interviewForApplicant(interviewId, applicant.getId());
		requireInterviewActionAllowed(interview);
		interview.decline();
		JobInterview saved = interviewRepository.save(interview);
		auditLogService.record("JOB_INTERVIEW_DECLINED", "JobInterview", String.valueOf(saved.getId()), actorUsername, "Interview declined", saved.getBusiness());
		sendInterviewDeclinedOwnerEmail(saved);
		return JobInterviewResponse.from(saved);
	}

	private JobPost requireJobPost(Long jobPostId) {
		return jobPostRepository.findById(jobPostId)
				.orElseThrow(() -> new ResourceNotFoundException("Job post not found: " + jobPostId));
	}

	private JobApplication ownerApplication(Long applicationId, Long businessId) {
		return applicationRepository.findByIdAndJobPost_Business_Id(applicationId, businessId)
				.orElseThrow(() -> new ResourceNotFoundException("Job application not found: " + applicationId));
	}

	private JobInterview interviewForApplicant(Long interviewId, Long applicantId) {
		return interviewRepository.findById(interviewId)
				.filter(interview -> interview.getApplicant().getId().equals(applicantId))
				.orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
	}

	private List<JobInterview> interviewsForApplicant(Long applicantId) {
		return interviewRepository.findAll().stream()
				.filter(interview -> interview.getApplicant().getId().equals(applicantId))
				.sorted((left, right) -> right.getInterviewDate().compareTo(left.getInterviewDate()))
				.toList();
	}

	private List<JobInterview> interviewsForBusiness(Long businessId) {
		return interviewRepository.findAll().stream()
				.filter(interview -> interview.getBusiness().getId().equals(businessId))
				.sorted((left, right) -> right.getInterviewDate().compareTo(left.getInterviewDate()))
				.toList();
	}

	private void requireInterviewActionAllowed(JobInterview interview) {
		if (interview.getStatus() != JobInterviewStatus.BOOKED) {
			throw new IllegalArgumentException("Interview has already been actioned");
		}
		if (interview.isExpired(OffsetDateTime.now())) {
			interview.expire();
			interviewRepository.save(interview);
			throw new IllegalArgumentException("Interview has expired");
		}
	}

	private void requireOwner(TrackerUser actor) {
		if (actor.getPrivilege() == null || actor.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can manage job posts");
		}
	}

	private void requireDateWindow(LocalDate startingDate, LocalDate closingDate) {
		if (closingDate.isBefore(startingDate)) {
			throw new IllegalArgumentException("Closing date cannot be before starting date");
		}
	}

	private List<String> normalizeInterests(List<String> interests) {
		List<String> normalized = normalizeOptionalList(interests);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("At least one job interest is required");
		}
		if (normalized.size() > MAX_INTERESTS) {
			throw new IllegalArgumentException("You can add up to five job interests");
		}
		return normalized;
	}

	private List<String> normalizeOptionalList(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
				.filter(StringUtils::hasText)
				.map(String::trim)
				.distinct()
				.toList();
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

	private BigDecimal normalizeMoney(BigDecimal value) {
		return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private String currencyFor(TrackerUser user) {
		LocalizationCountry localizationCountry = user.getLocalizationCountry();
		return localizationCountry == LocalizationCountry.SOUTH_AFRICA ? "ZAR" : "USD";
	}

	private String businessAddress(Business business) {
		String ownerAddress = business.getOwner() == null ? null : business.getOwner().getPhysicalAddress();
		if (!StringUtils.hasText(ownerAddress)) {
			throw new IllegalArgumentException("Business owner physical address is required before booking interviews");
		}
		return ownerAddress.trim();
	}

	private void sendApplicationSubmittedEmails(JobApplication application) {
		try {
			emailService.sendApplicationSubmittedOwnerEmail(application);
			emailService.sendApplicationSubmittedApplicantEmail(application);
		} catch (RuntimeException exception) {
			log.warn("job_application_email_failed_non_blocking applicationId={} reason={}", application.getId(), exception.getMessage());
		}
	}

	private void sendDeclinedEmail(JobApplication application) {
		try {
			emailService.sendApplicationDeclinedEmail(application);
		} catch (RuntimeException exception) {
			log.warn("job_application_declined_email_failed_non_blocking applicationId={} reason={}", application.getId(), exception.getMessage());
		}
	}

	private void sendInterviewBookedEmail(JobInterview interview) {
		try {
			emailService.sendInterviewBookedEmail(interview);
		} catch (RuntimeException exception) {
			log.warn("job_interview_booked_email_failed_non_blocking interviewId={} reason={}", interview.getId(), exception.getMessage());
		}
	}

	private void sendInterviewAcceptedOwnerEmail(JobInterview interview) {
		try {
			emailService.sendInterviewAcceptedOwnerEmail(interview);
		} catch (RuntimeException exception) {
			log.warn("job_interview_accepted_email_failed_non_blocking interviewId={} reason={}", interview.getId(), exception.getMessage());
		}
	}

	private void sendInterviewDeclinedOwnerEmail(JobInterview interview) {
		try {
			emailService.sendInterviewDeclinedOwnerEmail(interview);
		} catch (RuntimeException exception) {
			log.warn("job_interview_declined_email_failed_non_blocking interviewId={} reason={}", interview.getId(), exception.getMessage());
		}
	}
}
