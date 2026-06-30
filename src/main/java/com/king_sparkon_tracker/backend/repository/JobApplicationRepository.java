package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.JobApplication;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
	Optional<JobApplication> findByJobPost_IdAndApplicant_Id(Long jobPostId, Long applicantId);
	List<JobApplication> findByApplicant_IdOrderByCreatedDateDesc(Long applicantId);
	List<JobApplication> findByJobPost_Business_IdOrderByCreatedDateDesc(Long businessId);
	List<JobApplication> findByJobPost_IdOrderByCreatedDateDesc(Long jobPostId);
	Optional<JobApplication> findByIdAndJobPost_Business_Id(Long id, Long businessId);
}
