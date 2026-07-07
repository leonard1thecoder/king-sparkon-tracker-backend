package com.king_sparkon_tracker.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.king_sparkon_tracker.backend.model.DevHubDevelopmentRequest;
import com.king_sparkon_tracker.backend.model.DevHubRequestStatus;

public interface DevHubDevelopmentRequestRepository extends JpaRepository<DevHubDevelopmentRequest, Long> {

	@Query("""
			select request
			from DevHubDevelopmentRequest request
			where (:status is null or request.status = :status)
				and (
					:search is null
					or lower(coalesce(request.clientName, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(request.emailAddress, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(request.companyName, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(request.projectType, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(request.title, '')) like lower(concat('%', :search, '%'))
					or lower(coalesce(request.description, '')) like lower(concat('%', :search, '%'))
				)
			""")
	Page<DevHubDevelopmentRequest> search(
			@Param("status") DevHubRequestStatus status,
			@Param("search") String search,
			Pageable pageable);
}
