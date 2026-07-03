package com.king_sparkon_tracker.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.CreateSoftwareDevelopmentRequest;
import com.king_sparkon_tracker.backend.dto.DeveloperHubMetricsResponse;
import com.king_sparkon_tracker.backend.dto.UpdateSoftwareDevelopmentStageRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.DeveloperHubSoftwareRequest;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStage;
import com.king_sparkon_tracker.backend.model.SoftwareDevelopmentStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.DeveloperHubSoftwareRequestRepository;

@Service
@Transactional
public class DeveloperHubService {

	private static final Logger log = LoggerFactory.getLogger(DeveloperHubService.class);

	private final DeveloperHubSoftwareRequestRepository requestRepository;
	private final TrackerUserService userService;
	private final AuditLogService auditLogService;

	public DeveloperHubService(
			DeveloperHubSoftwareRequestRepository requestRepository,
			TrackerUserService userService,
			AuditLogService auditLogService) {
		this.requestRepository = requestRepository;
		this.userService = userService;
		this.auditLogService = auditLogService;
	}

	public DeveloperHubSoftwareRequest createSoftwareRequest(CreateSoftwareDevelopmentRequest request, String actorUsername) {
		TrackerUser owner = userService.getUserByUsername(actorUsername);
		requireRole(owner, PrivilegeRole.Owner, "Business owner role is required");
		Business business = requireBusiness(owner);
		DeveloperHubSoftwareRequest softwareRequest = requestRepository.save(new DeveloperHubSoftwareRequest(
				owner,
				business,
				normalizeRequired(request.softwareName(), "Software name is required"),
				normalizeRequired(request.softwareDescription(), "Software description is required"),
				request.requiresCloudMaintenance(),
				request.requiresQualityAssuranceRegression()));
		auditLogService.record(
				"DEVELOPER_HUB_SOFTWARE_REQUESTED",
				"DeveloperHubSoftwareRequest",
				String.valueOf(softwareRequest.getId()),
				actorUsername,
				"Software request created: " + softwareRequest.getSoftwareName(),
				business);
		log.info("developer_hub_request_created requestId={} businessId={} owner={}", softwareRequest.getId(), business.getId(), actorUsername);
		return softwareRequest;
	}

	@Transactional(readOnly = true)
	public List<DeveloperHubSoftwareRequest> listOwnerRequests(String actorUsername) {
		TrackerUser owner = userService.getUserByUsername(actorUsername);
		requireRole(owner, PrivilegeRole.Owner, "Business owner role is required");
		return requestRepository.findByBusiness_IdOrderByRequestedAtDesc(requireBusiness(owner).getId());
	}

	@Transactional(readOnly = true)
	public DeveloperHubMetricsResponse ownerMetrics(String actorUsername) {
		return DeveloperHubMetricsResponse.from(listOwnerRequests(actorUsername));
	}

	@Transactional(readOnly = true)
	public List<DeveloperHubSoftwareRequest> listAdminRequests(String actorUsername) {
		requireAdmin(actorUsername);
		return requestRepository.findAllByOrderByRequestedAtDesc();
	}

	@Transactional(readOnly = true)
	public DeveloperHubMetricsResponse adminMetrics(String actorUsername) {
		return DeveloperHubMetricsResponse.from(listAdminRequests(actorUsername));
	}

	public DeveloperHubSoftwareRequest updateStage(Long requestId, UpdateSoftwareDevelopmentStageRequest request, String actorUsername) {
		requireAdmin(actorUsername);
		Long id = requirePresent(requestId, "Software request id is required");
		SoftwareDevelopmentStage stage = requirePresent(request.stage(), "Developer Hub stage is required");
		SoftwareDevelopmentStatus status = request.status();
		DeveloperHubSoftwareRequest softwareRequest = requestRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Software request not found: " + id));
		softwareRequest.advanceTo(stage, status, normalizeOptional(request.adminNote()));
		DeveloperHubSoftwareRequest savedRequest = requestRepository.save(softwareRequest);
		auditLogService.record(
				"DEVELOPER_HUB_STAGE_UPDATED",
				"DeveloperHubSoftwareRequest",
				String.valueOf(savedRequest.getId()),
				actorUsername,
				"Developer Hub request moved to " + savedRequest.getStage() + " with status " + savedRequest.getStatus(),
				savedRequest.getBusiness());
		log.info("developer_hub_stage_updated requestId={} stage={} status={} actor={}", savedRequest.getId(), savedRequest.getStage(), savedRequest.getStatus(), actorUsername);
		return savedRequest;
	}

	private void requireAdmin(String actorUsername) {
		requireRole(userService.getUserByUsername(actorUsername), PrivilegeRole.Admin, "Admin role is required");
	}

	private Business requireBusiness(TrackerUser user) {
		if (user.getBusiness() == null) {
			throw new IllegalArgumentException("User is not linked to a business");
		}
		return user.getBusiness();
	}

	private void requireRole(TrackerUser user, PrivilegeRole role, String message) {
		if (user.getPrivilege() == null || user.getPrivilege().getName() != role) {
			throw new IllegalArgumentException(message);
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

	private <T> T requirePresent(T value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}
}
