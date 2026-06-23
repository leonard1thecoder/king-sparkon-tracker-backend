package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.WorkerPayoutAccount;

public interface WorkerPayoutAccountRepository extends JpaRepository<WorkerPayoutAccount, Long> {

	Optional<WorkerPayoutAccount> findByWorker_Id(Long workerId);
}
