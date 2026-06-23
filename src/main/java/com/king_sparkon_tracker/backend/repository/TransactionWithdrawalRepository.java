package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;

public interface TransactionWithdrawalRepository extends JpaRepository<TransactionWithdrawal, Long> {

	List<TransactionWithdrawal> findByBusinessIdOrderByRequestedAtDesc(Long businessId);
}
