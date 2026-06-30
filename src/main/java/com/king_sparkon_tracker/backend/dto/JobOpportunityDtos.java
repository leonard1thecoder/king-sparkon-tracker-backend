package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.JobApplication;
import com.king_sparkon_tracker.backend.model.JobApplicationStatus;
import com.king_sparkon_tracker.backend.model.JobExperienceLevel;
import com.king_sparkon_tracker.backend.model.JobInterview;
import com.king_sparkon_tracker.backend.model.JobInterviewStatus;
import com.king_sparkon_tracker.backend.model.JobPost;
import com.king_sparkon_tracker.backend.model.JobPostStatus;
import com.king_sparkon_tracker.backend.model.JobProfileAccessRequest;
import com.king_sparkon_tracker.backend.model.JobProfileAccessRequestStatus;
import com.king_sparkon_tracker.backend.model.JobSeekerProfile;
import com.king_sparkon_tracker.backend.model.QualificationLevel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class JobOpportunityDtos {
	private JobOpportunityDtos() {
	}

	public record UpsertJobSeekerProfileRequest(
			@NotNull QualificationLevel highestQualification,
			@NotEmpty @Size(min = 1, max = 5) List<@NotBlank @Size(max = 120) String> interestedJobs,
			@NotNull JobExperienceLevel experience,
			@NotBlank @Size(max = 2000) String about,
			boolean profileVisibleToBusinesses
	) {
	}

	public record JobSeekerProfileResponse(
			Long id,
			Long userId,
			String username,
			QualificationLevel highestQualification,
			List<String> interestedJobs,
			JobExperienceLevel experience,
			String about,
			boolean profileVisibleToBusinesses,
			boolean privateFieldsVisible,
			OffsetDateTime createdDate,
			OffsetDateTime modifiedDate
	) {
		public static JobSeekerProfileResponse from(JobSeekerProfile profile) {
			return visible(profile, true, true);
		}

		public static JobSeekerProfileResponse visible(JobSeekerProfile profile, boolean profileVisible, boolean privateFieldsVisible) {
			return new JobSeekerProfileResponse(
					profile.getId(),
					profile.getUser().getId(),
					profile.getUser().getUsername(),
					privateFieldsVisible ? profile.getHighestQualification() : null,
					profileVisible ? profile.getInterestedJobs() : List.of(),
					profileVisible ? profile.getExperience() : null,
					profileVisible ? profile.getAbout() : null,
					profile.isProfileVisibleToBusinesses(),
					privateFieldsVisible,
					profile.getCreatedDate(),
					profile.getModifiedDate());
		}
	}

	public record CreateJobPostRequest(
			@NotBlank @Size(max = 180) String title,
			@NotNull @FutureOrPresent LocalDate startingDate,
			@NotNull @FutureOrPresent LocalDate closingDate,
			@NotBlank @Size(max = 4000) String jobDescription,
			@NotNull JobExperienceLevel yearsOfExperienceRequired,
			@Size(max = 2048) String jobPostFileUrl,
			@DecimalMin("0.00") BigDecimal estimatedSalary
	) {
	}

	public record JobPostResponse(
			Long id,
			Long businessId,
			String businessName,
			String title,
			LocalDate startingDate,
			LocalDate closingDate,
			String jobDescription,
			JobExperienceLevel yearsOfExperienceRequired,
			String jobPostFileUrl,
			BigDecimal estimatedSalary,
			String currency,
			JobPostStatus status,
			OffsetDateTime createdDate,
			OffsetDateTime modifiedDate
	) {
		public static JobPostResponse from(JobPost post) {
			return new JobPostResponse(
					post.getId(),
					post.getBusiness().getId(),
					post.getBusiness().getName(),
					post.getTitle(),
					post.getStartingDate(),
					post.getClosingDate(),
					post.getJobDescription(),
					post.getYearsOfExperienceRequired(),
					post.getJobPostFileUrl(),
					post.getEstimatedSalary(),
					post.getCurrency(),
					post.getStatus(),
					post.getCreatedDate(),
					post.getModifiedDate());
		}
	}

	public record ApplyJobRequest(
			@NotBlank @Size(max = 2048) String resumeUrl,
			@Size(max = 10) List<@NotBlank @Size(max = 2048) String> certificateUrls
	) {
	}

	public record RequestProfileAccessRequest(
			@Size(max = 1000) String requestMessage
	) {
	}

	public record JobApplicationResponse(
			Long id,
			JobPostResponse jobPost,
			Long applicantId,
			String applicantUsername,
			String applicantEmail,
			JobSeekerProfileResponse profile,
			JobApplicationStatus status,
			String resumeUrl,
			List<String> certificateUrls,
			boolean privateProfileVisible,
			Long profileAccessRequestId,
			JobProfileAccessRequestStatus profileAccessRequestStatus,
			OffsetDateTime createdDate,
			OffsetDateTime modifiedDate,
			OffsetDateTime viewedDate,
			OffsetDateTime decisionDate
	) {
		public static JobApplicationResponse from(JobApplication application) {
			return visible(application, true, true, null);
		}

		public static JobApplicationResponse visible(
				JobApplication application,
				boolean profileVisible,
				boolean privateProfileVisible,
				JobProfileAccessRequest accessRequest) {
			return new JobApplicationResponse(
					application.getId(),
					JobPostResponse.from(application.getJobPost()),
					application.getApplicant().getId(),
					application.getApplicant().getUsername(),
					application.getApplicant().getEmailAddress(),
					JobSeekerProfileResponse.visible(application.getProfile(), profileVisible || privateProfileVisible, privateProfileVisible),
					application.getStatus(),
					application.getResumeUrl(),
					privateProfileVisible ? application.getCertificateUrls() : List.of(),
					privateProfileVisible,
					accessRequest == null ? null : accessRequest.getId(),
					accessRequest == null ? null : accessRequest.getStatus(),
					application.getCreatedDate(),
					application.getModifiedDate(),
					application.getViewedDate(),
					application.getDecisionDate());
		}
	}

	public record JobProfileAccessRequestResponse(
			Long id,
			Long applicationId,
			Long jobPostId,
			String jobTitle,
			Long businessId,
			String businessName,
			Long applicantId,
			String applicantUsername,
			String applicantEmail,
			JobProfileAccessRequestStatus status,
			String requestMessage,
			OffsetDateTime createdDate,
			OffsetDateTime modifiedDate,
			OffsetDateTime respondedDate
	) {
		public static JobProfileAccessRequestResponse from(JobProfileAccessRequest request) {
			return new JobProfileAccessRequestResponse(
					request.getId(),
					request.getApplication().getId(),
					request.getApplication().getJobPost().getId(),
					request.getApplication().getJobPost().getTitle(),
					request.getBusiness().getId(),
					request.getBusiness().getName(),
					request.getApplicant().getId(),
					request.getApplicant().getUsername(),
					request.getApplicant().getEmailAddress(),
					request.getStatus(),
					request.getRequestMessage(),
					request.getCreatedDate(),
					request.getModifiedDate(),
					request.getRespondedDate());
		}
	}

	public record BookInterviewRequest(
			@NotNull @Future OffsetDateTime interviewDate,
			@NotBlank @Size(max = 2000) String interviewDescription,
			@NotNull @Future OffsetDateTime interviewExpiresAt
	) {
	}

	public record JobInterviewResponse(
			Long id,
			Long applicationId,
			JobPostResponse jobPost,
			Long applicantId,
			String applicantUsername,
			String applicantEmail,
			OffsetDateTime interviewDate,
			String interviewDescription,
			OffsetDateTime interviewExpiresAt,
			String businessAddress,
			JobInterviewStatus status,
			OffsetDateTime createdDate,
			OffsetDateTime modifiedDate,
			OffsetDateTime respondedDate
	) {
		public static JobInterviewResponse from(JobInterview interview) {
			return new JobInterviewResponse(
					interview.getId(),
					interview.getApplication().getId(),
					JobPostResponse.from(interview.getJobPost()),
					interview.getApplicant().getId(),
					interview.getApplicant().getUsername(),
					interview.getApplicant().getEmailAddress(),
					interview.getInterviewDate(),
					interview.getInterviewDescription(),
					interview.getInterviewExpiresAt(),
					interview.getBusinessAddress(),
					interview.getStatus(),
					interview.getCreatedDate(),
					interview.getModifiedDate(),
					interview.getRespondedDate());
		}
	}

	public record OpportunitiesResponse(
			List<JobPostResponse> jobPosts,
			List<JobApplicationResponse> applications,
			List<JobInterviewResponse> interviews,
			List<JobProfileAccessRequestResponse> profileAccessRequests
	) {
	}
}
