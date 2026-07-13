package com.king_sparkon_tracker.backend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.BusinessWithdrawal;
import com.king_sparkon_tracker.backend.model.BusinessWithdrawalStatus;

public interface BusinessWithdrawalRepository extends JpaRepository<BusinessWithdrawal, Long> {

	List<BusinessWithdrawal> findByBusiness_IdOrderByRequestedAtDesc(Long businessId);

	long countByBusiness_IdAndStatusIn(Long businessId, Collection<BusinessWithdrawalStatus> statuses);
}
