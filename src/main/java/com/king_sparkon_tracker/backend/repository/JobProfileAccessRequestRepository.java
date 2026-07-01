package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.JobProfileAccessRequest;
import com.king_sparkon_tracker.backend.model.JobProfileAccessRequestStatus;

public interface JobProfileAccessRequestRepository extends JpaRepository<JobProfileAccessRequest, Long> {
	Optional<JobProfileAccessRequest> findByApplication_Id(Long applicationId);
	Optional<JobProfileAccessRequest> findByIdAndApplicant_Id(Long id, Long applicantId);
	List<JobProfileAccessRequest> findByApplicant_IdOrderByCreatedDateDesc(Long applicantId);
	List<JobProfileAccessRequest> findByBusiness_IdOrderByCreatedDateDesc(Long businessId);
	boolean existsByApplication_IdAndStatus(Long applicationId, JobProfileAccessRequestStatus status);
}
