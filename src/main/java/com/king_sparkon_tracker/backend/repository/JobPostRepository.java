package com.king_sparkon_tracker.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.JobPost;
import com.king_sparkon_tracker.backend.model.JobPostStatus;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {
	List<JobPost> findByBusiness_IdOrderByCreatedDateDesc(Long businessId);
	List<JobPost> findByStatusAndClosingDateGreaterThanEqualOrderByCreatedDateDesc(JobPostStatus status, LocalDate closingDate);
	Optional<JobPost> findByIdAndBusiness_Id(Long id, Long businessId);
}
