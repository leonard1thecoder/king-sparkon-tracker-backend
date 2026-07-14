package com.king_sparkon_tracker.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.king_sparkon_tracker.backend.dto.AffiliatePosterResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.AffiliatePoster;
import com.king_sparkon_tracker.backend.model.AffiliatePosterCategory;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.AffiliatePosterRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class AffiliatePosterService {

	private final AffiliatePosterRepository posterRepository;
	private final TrackerUserRepository userRepository;
	private final GoogleStorageService googleStorageService;

	public AffiliatePosterService(
			AffiliatePosterRepository posterRepository,
			TrackerUserRepository userRepository,
			GoogleStorageService googleStorageService) {
		this.posterRepository = posterRepository;
		this.userRepository = userRepository;
		this.googleStorageService = googleStorageService;
	}

	public AffiliatePosterResponse upload(
			AffiliatePosterCategory category,
			String title,
			String description,
			MultipartFile file,
			String actorUsername) {
		TrackerUser administrator = requireRole(actorUsername, PrivilegeRole.Admin);
		if (category == null) {
			throw new IllegalArgumentException("Poster category is required");
		}
		String normalizedTitle = required(title, "Poster title is required");
		GoogleStorageService.StoredImage image = googleStorageService.storeImage(
				file,
				"affiliate-posters/" + category.name().toLowerCase(),
				administrator.getUsername());
		AffiliatePoster poster = posterRepository.save(new AffiliatePoster(
				category,
				normalizedTitle,
				optional(description),
				image.url(),
				image.objectName(),
				administrator.getUsername()));
		return AffiliatePosterResponse.from(poster);
	}

	@Transactional(readOnly = true)
	public List<AffiliatePosterResponse> listActive() {
		return posterRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
				.map(AffiliatePosterResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AffiliatePosterResponse> listAdmin(String actorUsername) {
		requireRole(actorUsername, PrivilegeRole.Admin);
		return posterRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(AffiliatePosterResponse::from)
				.toList();
	}

	public void deactivate(Long posterId, String actorUsername) {
		requireRole(actorUsername, PrivilegeRole.Admin);
		if (posterId == null) {
			throw new IllegalArgumentException("Poster id is required");
		}
		AffiliatePoster poster = posterRepository.findById(posterId)
				.orElseThrow(() -> new ResourceNotFoundException("Affiliate poster not found: " + posterId));
		poster.deactivate();
		posterRepository.save(poster);
	}

	private TrackerUser requireRole(String username, PrivilegeRole role) {
		String normalized = required(username, "Authenticated username is required");
		TrackerUser user = userRepository.findByUsername(normalized)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalized));
		if (user.getPrivilege() == null || user.getPrivilege().getName() != role) {
			throw new IllegalArgumentException("Only administrators can manage affiliate posters");
		}
		return user;
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String optional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
