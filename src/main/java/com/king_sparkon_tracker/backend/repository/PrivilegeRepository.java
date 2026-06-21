package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

	Optional<Privilege> findByName(PrivilegeRole name);

	boolean existsByName(PrivilegeRole name);
}
