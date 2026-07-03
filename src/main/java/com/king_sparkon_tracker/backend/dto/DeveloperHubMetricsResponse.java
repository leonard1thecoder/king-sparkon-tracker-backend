package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import com.king_sparkon_tracker.backend.model.DeveloperHubSoftwareRequest;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStage;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeveloperHubMetricsResponse(
		@Schema(description = "Total software development requests in scope.", example = "12")
		long requestedBuilds,
		@Schema(description = "Requests where admin moved beyond REQUESTED.", example = "5")
		long processStarted,
		@Schema(description = "Requests requiring QA regression.", example = "8")
		long qaRegression,
		@Schema(description = "Requests requiring cloud maintenance.", example = "7")
		long cloudMaintenance,
		@Schema(description = "Completed requests.", example = "2")
		long completed,
		@Schema(description = "Requests not completed yet.", example = "10")
		long active
) {
	public static DeveloperHubMetricsResponse from(List<DeveloperHubSoftwareRequest> requests) {
		long completed = requests.stream()
				.filter(request -> request.getStatus() == SoftwareDevelopmentStatus.COMPLETED)
				.count();
		return new DeveloperHubMetricsResponse(
				requests.size(),
				requests.stream().filter(request -> request.getStage() != SoftwareDevelopmentStage.REQUESTED).count(),
				requests.stream().filter(DeveloperHubSoftwareRequest::isRequiresQualityAssuranceRegression).count(),
				requests.stream().filter(DeveloperHubSoftwareRequest::isRequiresCloudMaintenance).count(),
				completed,
				requests.size() - completed
		);
	}
}
