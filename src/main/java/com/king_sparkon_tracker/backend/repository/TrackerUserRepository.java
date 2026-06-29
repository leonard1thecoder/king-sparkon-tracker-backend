package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;

public interface TrackerUserRepository extends JpaRepository<TrackerUser, Long> {

	Optional<TrackerUser> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByEmailAddress(String emailAddress);

	boolean existsByAffiliateCode(String affiliateCode);

	long countByPrivilege_Name(PrivilegeRole privilegeName);

	long countByBusiness_IdAndPrivilege_Name(Long businessId, PrivilegeRole privilegeName);

	Page<TrackerUser> findByBusiness_Id(Long businessId, Pageable pageable);

	List<TrackerUser> findByBusiness_IdAndPrivilege_NameOrderByUsernameAsc(Long businessId, PrivilegeRole privilegeName);

	Optional<TrackerUser> findByIdAndBusiness_Id(Long id, Long businessId);

	Optional<TrackerUser> findByEmailAddress(String emailAddress);

	Optional<TrackerUser> findByAffiliateCode(String affiliateCode);

}
