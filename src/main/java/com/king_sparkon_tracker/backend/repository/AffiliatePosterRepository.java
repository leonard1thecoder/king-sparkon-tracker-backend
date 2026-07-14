package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.AffiliatePoster;

public interface AffiliatePosterRepository extends JpaRepository<AffiliatePoster, Long> {

	List<AffiliatePoster> findByActiveTrueOrderByCreatedAtDesc();

	List<AffiliatePoster> findAllByOrderByCreatedAtDesc();
}
