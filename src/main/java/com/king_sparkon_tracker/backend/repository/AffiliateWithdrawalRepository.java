package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.AffiliateWithdrawal;

public interface AffiliateWithdrawalRepository extends JpaRepository<AffiliateWithdrawal, Long> {

	List<AffiliateWithdrawal> findByAffiliate_IdOrderByRequestedAtDesc(Long affiliateId);
}
