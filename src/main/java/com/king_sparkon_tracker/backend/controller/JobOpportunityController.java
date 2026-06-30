package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.ApplyJobRequest;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobApplicationResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobInterviewResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobPostResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.JobSeekerProfileResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.OpportunitiesResponse;
import com.king_sparkon_tracker.backend.dto.JobOpportunityDtos.UpsertJobSeekerProfileRequest;
import com.king_sparkon_tracker.backend.service.JobOpportunityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/opportunities")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Opportunities", description = "Worker and user job opportunities, applications, and interview decisions.")
public class JobOpportunityController {

	private final JobOpportunityService jobOpportunityService;

	public JobOpportunityController(JobOpportunityService jobOpportunityService) {
		this.jobOpportunityService = jobOpportunityService;
	}

	@GetMapping
	@Operation(summary = "Opportunities navigation", description = "Returns open job posts, my applications, and my interviews for the Opportunities nav.")
	public OpportunitiesResponse opportunities(Principal principal) {
		return jobOpportunityService.opportunities(principal.getName());
	}

	@GetMapping("/profile")
	@Operation(summary = "My opportunity profile", description = "Returns the current user's job seeker profile.")
	public JobSeekerProfileResponse profile(Principal principal) {
		return jobOpportunityService.profile(principal.getName());
	}

	@PutMapping("/profile")
	@Operation(summary = "Create or update opportunity profile", description = "Creates or updates qualification, interests, experience, and about profile data.")
	public JobSeekerProfileResponse upsertProfile(
			@Valid @RequestBody UpsertJobSeekerProfileRequest request,
			Principal principal) {
		return jobOpportunityService.upsertProfile(request, principal.getName());
	}

	@GetMapping("/jobs")
	@Operation(summary = "Open job posts", description = "Lists open job posts that are still accepting applications.")
	public List<JobPostResponse> jobs() {
		return jobOpportunityService.openJobPosts();
	}

	@GetMapping("/jobs/{jobPostId}")
	@Operation(summary = "View job post", description = "Returns one job post for viewing before applying.")
	public JobPostResponse job(@PathVariable Long jobPostId) {
		return jobOpportunityService.jobPost(jobPostId);
	}

	@PostMapping("/jobs/{jobPostId}/apply")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Apply for a job", description = "Applies to a job post with a required resume URL and optional certificate URLs.")
	public JobApplicationResponse apply(
			@PathVariable Long jobPostId,
			@Valid @RequestBody ApplyJobRequest request,
			Principal principal) {
		return jobOpportunityService.apply(jobPostId, request, principal.getName());
	}

	@GetMapping("/applications")
	@Operation(summary = "My applications", description = "Lists job applications for the current user.")
	public List<JobApplicationResponse> myApplications(Principal principal) {
		return jobOpportunityService.myApplications(principal.getName());
	}

	@GetMapping("/interviews")
	@Operation(summary = "My interviews", description = "Lists interviews booked for the current user.")
	public List<JobInterviewResponse> myInterviews(Principal principal) {
		return jobOpportunityService.myInterviews(principal.getName());
	}

	@PostMapping("/interviews/{interviewId}/accept")
	@Operation(summary = "Accept interview", description = "Accepts a booked interview if it has not expired.")
	public JobInterviewResponse acceptInterview(@PathVariable Long interviewId, Principal principal) {
		return jobOpportunityService.acceptInterview(interviewId, principal.getName());
	}

	@PostMapping("/interviews/{interviewId}/reject")
	@Operation(summary = "Reject interview", description = "Declines a booked interview if it has not expired.")
	public JobInterviewResponse rejectInterview(@PathVariable Long interviewId, Principal principal) {
		return jobOpportunityService.declineInterview(interviewId, principal.getName());
	}
}
