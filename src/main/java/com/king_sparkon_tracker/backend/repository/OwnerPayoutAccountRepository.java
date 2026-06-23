package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.OwnerPayoutAccount;

public interface OwnerPayoutAccountRepository extends JpaRepository<OwnerPayoutAccount, Long> {

	Optional<OwnerPayoutAccount> findByBusinessId(Long businessId);
}
