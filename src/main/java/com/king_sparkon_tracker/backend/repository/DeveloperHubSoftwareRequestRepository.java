package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.DeveloperHubSoftwareRequest;

public interface DeveloperHubSoftwareRequestRepository extends JpaRepository<DeveloperHubSoftwareRequest, Long> {

	List<DeveloperHubSoftwareRequest> findByBusiness_IdOrderByRequestedAtDesc(Long businessId);

	List<DeveloperHubSoftwareRequest> findAllByOrderByRequestedAtDesc();

	Optional<DeveloperHubSoftwareRequest> findByIdAndBusiness_Id(Long id, Long businessId);
}
