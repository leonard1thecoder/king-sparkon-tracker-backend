package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.BookInterviewRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.CreateJobPostRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobApplicationResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobInterviewResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobPostResponse;
import com.king_sparkon_tracker.backend.service.JobOpportunityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owner/job-posts")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Owner Job Posts", description = "Owner job post creation, application review, and interview booking.")
public class OwnerJobPostController {

	private final JobOpportunityService jobOpportunityService;

	public OwnerJobPostController(JobOpportunityService jobOpportunityService) {
		this.jobOpportunityService = jobOpportunityService;
	}

	@GetMapping
	@Operation(summary = "Owner job posts", description = "Lists job posts for the current owner business.")
	public List<JobPostResponse> jobPosts(Principal principal) {
		return jobOpportunityService.ownerJobPosts(principal.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create owner job post", description = "Creates an open job post for the current owner business.")
	public JobPostResponse createJobPost(
			@Valid @RequestBody CreateJobPostRequest request,
			Principal principal) {
		return jobOpportunityService.createJobPost(request, principal.getName());
	}

	@GetMapping("/applications")
	@Operation(summary = "Owner applications", description = "Lists applications across all owner business job posts.")
	public List<JobApplicationResponse> applications(Principal principal) {
		return jobOpportunityService.ownerApplications(principal.getName());
	}

	@GetMapping("/{jobPostId}/applications")
	@Operation(summary = "Applications for one job post", description = "Lists applications for a single owner job post.")
	public List<JobApplicationResponse> applicationsForJob(
			@PathVariable Long jobPostId,
			Principal principal) {
		return jobOpportunityService.ownerApplicationsForJob(jobPostId, principal.getName());
	}

	@PostMapping("/applications/{applicationId}/view")
	@Operation(summary = "Mark application viewed", description = "Marks an application as viewed after the owner opens it.")
	public JobApplicationResponse viewApplication(
			@PathVariable Long applicationId,
			Principal principal) {
		return jobOpportunityService.viewApplication(applicationId, principal.getName());
	}

	@PostMapping("/applications/{applicationId}/reject")
	@Operation(summary = "Reject application", description = "Rejects an application and emails the applicant.")
	public JobApplicationResponse rejectApplication(
			@PathVariable Long applicationId,
			Principal principal) {
		return jobOpportunityService.declineApplication(applicationId, principal.getName());
	}

	@PostMapping("/applications/{applicationId}/interview")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Book interview", description = "Accepts an application by booking an interview with date, description, expiry, and business address.")
	public JobInterviewResponse bookInterview(
			@PathVariable Long applicationId,
			@Valid @RequestBody BookInterviewRequest request,
			Principal principal) {
		return jobOpportunityService.bookInterview(applicationId, request, principal.getName());
	}

	@GetMapping("/interviews")
	@Operation(summary = "Owner interviews", description = "Lists interviews for the current owner business.")
	public List<JobInterviewResponse> interviews(Principal principal) {
		return jobOpportunityService.ownerInterviews(principal.getName());
	}
}
