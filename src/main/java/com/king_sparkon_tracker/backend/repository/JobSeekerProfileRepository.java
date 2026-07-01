package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.JobSeekerProfile;

public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, Long> {
	Optional<JobSeekerProfile> findByUser_Id(Long userId);
}
