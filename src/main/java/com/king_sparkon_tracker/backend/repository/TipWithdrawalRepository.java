package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.TipWithdrawal;

public interface TipWithdrawalRepository extends JpaRepository<TipWithdrawal, Long> {

	List<TipWithdrawal> findByWorker_IdOrderByRequestedAtDesc(Long workerId);
}
