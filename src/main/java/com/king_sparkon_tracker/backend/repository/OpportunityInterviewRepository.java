package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.JobInterview;

public interface OpportunityInterviewRepository extends JpaRepository<JobInterview, Long> {
	Optional<JobInterview> findByApplication_Id(Long applicationId);
	List<JobInterview> findByApplicant_IdOrderByInterviewDateDesc(Long applicantId);
	List<JobInterview> findByBusiness_IdOrderByInterviewDateDesc(Long businessId);
	Optional<JobInterview> findByIdAndApplicant_Id(Long id, Long applicantId);
	Optional<JobInterview> findByIdAndBusiness_Id(Long id, Long businessId);
}
