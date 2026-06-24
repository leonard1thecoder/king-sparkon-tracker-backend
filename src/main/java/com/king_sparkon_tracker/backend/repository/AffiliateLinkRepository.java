package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.AffiliateLink;
import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;

public interface AffiliateLinkRepository extends JpaRepository<AffiliateLink, Long> {

	List<AffiliateLink> findByStatusAndPlacement(AffiliateLinkStatus status, AffiliatePlacement placement);
}
