package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.ApplyJobRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.BookInterviewRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.CreateJobPostRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobApplicationResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobInterviewResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobPostResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobProfileAccessRequestResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.RequestProfileAccessRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.UpsertJobSeekerProfileRequest;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.JobApplication;
import com.king_sparkon_tracker.backend.model.JobApplicationStatus;
import com.king_sparkon_tracker.backend.model.JobExperienceLevel;
import com.king_sparkon_tracker.backend.model.JobInterview;
import com.king_sparkon_tracker.backend.model.JobInterviewStatus;
import com.king_sparkon_tracker.backend.model.JobPost;
import com.king_sparkon_tracker.backend.model.JobProfileAccessRequest;
import com.king_sparkon_tracker.backend.model.JobProfileAccessRequestStatus;
import com.king_sparkon_tracker.backend.model.JobSeekerProfile;
import com.king_sparkon_tracker.backend.model.LocalizationCountry;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.QualificationLevel;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.JobApplicationRepository;
import com.king_sparkon_tracker.backend.repository.JobPostRepository;
import com.king_sparkon_tracker.backend.repository.JobProfileAccessRequestRepository;
import com.king_sparkon_tracker.backend.repository.JobSeekerProfileRepository;
import com.king_sparkon_tracker.backend.repository.OpportunityInterviewRepository;

@ExtendWith(MockitoExtension.class)
class JobOpportunityServiceTest {

	@Mock
	private JobSeekerProfileRepository profileRepository;

	@Mock
	private JobPostRepository jobPostRepository;

	@Mock
	private JobApplicationRepository applicationRepository;

	@Mock
	private OpportunityInterviewRepository interviewRepository;

	@Mock
	private JobProfileAccessRequestRepository accessRequestRepository;

	@Mock
	private TrackerUserService trackerUserService;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private JobOpportunityEmailService emailService;

	private JobOpportunityService service;

	@BeforeEach
	void setUp() {
		service = new JobOpportunityService(
				profileRepository,
				jobPostRepository,
				applicationRepository,
				interviewRepository,
				accessRequestRepository,
				trackerUserService,
				auditLogService,
				emailService);
	}

	@Test
	void upsertProfileRejectsMoreThanFiveInterests() {
		TrackerUser user = user(10L, "applicant", PrivilegeRole.User);
		when(trackerUserService.getUserByUsername("applicant")).thenReturn(user);

		UpsertJobSeekerProfileRequest request = new UpsertJobSeekerProfileRequest(
				QualificationLevel.GRADE_12,
				List.of("Cashier", "Stock Controller", "Waiter", "Driver", "Cleaner", "Admin"),
				JobExperienceLevel.ONE_YEAR,
				"Hard worker",
				false);

		assertThatThrownBy(() -> service.upsertProfile(request, "applicant"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("up to five");
	}

	@Test
	void createJobPostUsesOwnerBusinessAndLocalizedCurrency() {
		TrackerUser owner = owner();
		Business business = business(owner);
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
		when(trackerUserService.businessForActor("owner")).thenReturn(business);
		when(jobPostRepository.save(any(JobPost.class))).thenAnswer(invocation -> {
			JobPost post = invocation.getArgument(0);
			ReflectionTestUtils.setField(post, "id", 99L);
			return post;
		});

		JobPostResponse response = service.createJobPost(new CreateJobPostRequest(
				"Junior Scanner Operator",
				LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(14),
				"Scan products and support stock control.",
				JobExperienceLevel.LESS_THAN_ONE_YEAR,
				"https://cdn.example.com/post.pdf",
				new BigDecimal("4500")), "owner");

		assertThat(response.id()).isEqualTo(99L);
		assertThat(response.businessId()).isEqualTo(1L);
		assertThat(response.currency()).isEqualTo("ZAR");
		assertThat(response.estimatedSalary()).isEqualByComparingTo("4500.00");
	}

	@Test
	void applyRequiresExistingOpportunityProfile() {
		TrackerUser applicant = user(11L, "applicant", PrivilegeRole.User);
		when(trackerUserService.getUserByUsername("applicant")).thenReturn(applicant);
		when(profileRepository.findByUser_Id(11L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.apply(100L, new ApplyJobRequest("https://cdn.example.com/cv.pdf", List.of()), "applicant"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("profile before applying");
	}

	@Test
	void applyCreatesApplicationAndSendsEmails() {
		TrackerUser owner = owner();
		Business business = business(owner);
		TrackerUser applicant = user(11L, "applicant", PrivilegeRole.User);
		JobPost post = jobPost(business, owner);
		JobSeekerProfile profile = profile(applicant, false);

		when(trackerUserService.getUserByUsername("applicant")).thenReturn(applicant);
		when(profileRepository.findByUser_Id(11L)).thenReturn(Optional.of(profile));
		when(jobPostRepository.findById(100L)).thenReturn(Optional.of(post));
		when(applicationRepository.findByJobPost_IdAndApplicant_Id(100L, 11L)).thenReturn(Optional.empty());
		when(applicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> {
			JobApplication application = invocation.getArgument(0);
			ReflectionTestUtils.setField(application, "id", 200L);
			return application;
		});

		JobApplicationResponse response = service.apply(100L, new ApplyJobRequest(
				"https://cdn.example.com/cv.pdf",
				List.of("https://cdn.example.com/cert.pdf")), "applicant");

		assertThat(response.id()).isEqualTo(200L);
		assertThat(response.status()).isEqualTo(JobApplicationStatus.SUBMITTED);
		assertThat(response.certificateUrls()).containsExactly("https://cdn.example.com/cert.pdf");
		verify(emailService).sendApplicationSubmittedOwnerEmail(any(JobApplication.class));
		verify(emailService).sendApplicationSubmittedApplicantEmail(any(JobApplication.class));
	}

	@Test
	void ownerApplicationResponseHidesProfileQualificationAndCertsUntilApproved() {
		TrackerUser owner = owner();
		Business business = business(owner);
		JobApplication application = application(business, owner, false);
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
		when(trackerUserService.businessForActor("owner")).thenReturn(business);
		when(applicationRepository.findByJobPost_Business_IdOrderByCreatedDateDesc(1L)).thenReturn(List.of(application));
		when(accessRequestRepository.findByApplication_Id(200L)).thenReturn(Optional.empty());

		JobApplicationResponse response = service.ownerApplications("owner").getFirst();

		assertThat(response.profile().highestQualification()).isNull();
		assertThat(response.profile().interestedJobs()).isEmpty();
		assertThat(response.certificateUrls()).isEmpty();
		assertThat(response.privateProfileVisible()).isFalse();
	}

	@Test
	void ownerCanRequestAndApplicantCanApprovePrivateProfileAccess() {
		TrackerUser owner = owner();
		Business business = business(owner);
		JobApplication application = application(business, owner, false);
		JobProfileAccessRequest request = new JobProfileAccessRequest(application, owner, "Please review qualification documents.");
		ReflectionTestUtils.setField(request, "id", 301L);
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
		when(trackerUserService.businessForActor("owner")).thenReturn(business);
		when(applicationRepository.findByIdAndJobPost_Business_Id(200L, 1L)).thenReturn(Optional.of(application));
		when(accessRequestRepository.findByApplication_Id(200L)).thenReturn(Optional.empty());
		when(accessRequestRepository.save(any(JobProfileAccessRequest.class))).thenReturn(request);

		JobProfileAccessRequestResponse requested = service.requestProfileAccess(
				200L,
				new RequestProfileAccessRequest("Please review qualification documents."),
				"owner");

		assertThat(requested.id()).isEqualTo(301L);
		assertThat(requested.status()).isEqualTo(JobProfileAccessRequestStatus.REQUESTED);
		verify(emailService).sendProfileAccessRequestedEmail(any(JobProfileAccessRequest.class));

		TrackerUser applicant = application.getApplicant();
		when(trackerUserService.getUserByUsername("applicant")).thenReturn(applicant);
		when(accessRequestRepository.findByIdAndApplicant_Id(301L, applicant.getId())).thenReturn(Optional.of(request));
		when(accessRequestRepository.save(request)).thenReturn(request);

		JobProfileAccessRequestResponse approved = service.approveProfileAccess(301L, "applicant");

		assertThat(approved.status()).isEqualTo(JobProfileAccessRequestStatus.APPROVED);
		verify(emailService).sendProfileAccessApprovedEmail(request);
	}

	@Test
	void ownerViewMarksApplicationViewed() {
		TrackerUser owner = owner();
		Business business = business(owner);
		JobApplication application = application(business, owner, false);
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
		when(trackerUserService.businessForActor("owner")).thenReturn(business);
		when(applicationRepository.findByIdAndJobPost_Business_Id(200L, 1L)).thenReturn(Optional.of(application));
		when(applicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(accessRequestRepository.findByApplication_Id(200L)).thenReturn(Optional.empty());

		JobApplicationResponse response = service.viewApplication(200L, "owner");

		assertThat(response.status()).isEqualTo(JobApplicationStatus.VIEWED);
		assertThat(response.viewedDate()).isNotNull();
	}

	@Test
	void bookInterviewCreatesBookedInterviewAndMovesApplicationStatus() {
		TrackerUser owner = owner();
		Business business = business(owner);
		JobApplication application = application(business, owner, false);
		when(trackerUserService.getUserByUsername("owner")).thenReturn(owner);
		when(trackerUserService.businessForActor("owner")).thenReturn(business);
		when(applicationRepository.findByIdAndJobPost_Business_Id(200L, 1L)).thenReturn(Optional.of(application));
		when(interviewRepository.findByApplication_Id(200L)).thenReturn(Optional.empty());
		when(applicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(interviewRepository.save(any(JobInterview.class))).thenAnswer(invocation -> {
			JobInterview interview = invocation.getArgument(0);
			ReflectionTestUtils.setField(interview, "id", 300L);
			return interview;
		});

		OffsetDateTime interviewDate = OffsetDateTime.now().plusDays(3);
		JobInterviewResponse response = service.bookInterview(200L, new BookInterviewRequest(
				interviewDate,
				"Bring your documents.",
				interviewDate.minusDays(1)), "owner");

		assertThat(application.getStatus()).isEqualTo(JobApplicationStatus.INTERVIEW_BOOKED);
		assertThat(response.id()).isEqualTo(300L);
		assertThat(response.status()).isEqualTo(JobInterviewStatus.BOOKED);
		assertThat(response.businessAddress()).isEqualTo("123 King Street");
		verify(emailService).sendInterviewBookedEmail(any(JobInterview.class));
	}

	@Test
	void interviewExpiryBlocksResponse() {
		TrackerUser owner = owner();
		Business business = business(owner);
		JobApplication application = application(business, owner, false);
		JobInterview interview = new JobInterview(
				application,
				OffsetDateTime.now().plusDays(1),
				"Interview details",
				OffsetDateTime.now().minusHours(1),
				"123 King Street");
		ReflectionTestUtils.setField(interview, "id", 300L);
		when(trackerUserService.getUserByUsername("applicant")).thenReturn(application.getApplicant());
		when(interviewRepository.findByIdAndApplicant_Id(300L, 11L)).thenReturn(Optional.of(interview));
		when(interviewRepository.save(any(JobInterview.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThatThrownBy(() -> service.acceptInterview(300L, "applicant"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("expired");
		assertThat(interview.getStatus()).isEqualTo(JobInterviewStatus.EXPIRED);
	}

	private TrackerUser owner() {
		TrackerUser owner = user(7L, "owner", PrivilegeRole.Owner);
		owner.completeOnboarding("123 King Street", "+27110000000");
		return owner;
	}

	private TrackerUser user(Long id, String username, PrivilegeRole role) {
		TrackerUser user = new TrackerUser(username, username + "@example.com", "secret", new Privilege(role), LocalizationCountry.SOUTH_AFRICA);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Business business(TrackerUser owner) {
		Business business = new Business("Sparkon Logistics", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		return business;
	}

	private JobPost jobPost(Business business, TrackerUser owner) {
		JobPost post = new JobPost(
				business,
				owner,
				"Junior Scanner Operator",
				LocalDate.now().plusDays(1),
				LocalDate.now().plusDays(14),
				"Scan products and support stock control.",
				JobExperienceLevel.LESS_THAN_ONE_YEAR,
				"https://cdn.example.com/post.pdf",
				new BigDecimal("4500.00"),
				"ZAR");
		ReflectionTestUtils.setField(post, "id", 100L);
		return post;
	}

	private JobSeekerProfile profile(TrackerUser applicant, boolean profileVisibleToBusinesses) {
		JobSeekerProfile profile = new JobSeekerProfile(
				applicant,
				QualificationLevel.GRADE_12,
				List.of("Cashier", "Stock Controller"),
				JobExperienceLevel.ONE_YEAR,
				"Reliable and fast learner.",
				profileVisibleToBusinesses);
		ReflectionTestUtils.setField(profile, "id", 50L);
		return profile;
	}

	private JobApplication application(Business business, TrackerUser owner, boolean profileVisibleToBusinesses) {
		TrackerUser applicant = user(11L, "applicant", PrivilegeRole.User);
		JobApplication application = new JobApplication(
				jobPost(business, owner),
				applicant,
				profile(applicant, profileVisibleToBusinesses),
				"https://cdn.example.com/cv.pdf",
				List.of("https://cdn.example.com/cert.pdf"));
		ReflectionTestUtils.setField(application, "id", 200L);
		return application;
	}
}
