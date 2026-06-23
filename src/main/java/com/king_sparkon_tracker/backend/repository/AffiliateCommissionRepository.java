package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.AffiliateCommission;
import com.king_sparkon_tracker.backend.model.AffiliateCommissionStatus;

public interface AffiliateCommissionRepository extends JpaRepository<AffiliateCommission, Long> {

	boolean existsBySubscription_Id(Long subscriptionId);

	List<AffiliateCommission> findByAffiliate_IdOrderByEarnedAtDesc(Long affiliateId);

	List<AffiliateCommission> findByAffiliate_IdAndStatusAndWithdrawalIsNullOrderByEarnedAtAsc(
			Long affiliateId,
			AffiliateCommissionStatus status);
}
