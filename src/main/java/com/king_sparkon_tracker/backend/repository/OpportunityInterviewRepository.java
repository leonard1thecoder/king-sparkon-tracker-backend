package com.king_sparkon_tracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.JobInterview;

public interface OpportunityInterviewRepository extends JpaRepository<JobInterview, Long> {
}
