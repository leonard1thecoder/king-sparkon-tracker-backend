package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.BillingPlanDiscount;
import com.king_sparkon_tracker.backend.model.BusinessPlan;

public interface BillingPlanDiscountRepository extends JpaRepository<BillingPlanDiscount, Long> {

	Optional<BillingPlanDiscount> findByBusinessPlan(BusinessPlan businessPlan);

	List<BillingPlanDiscount> findAllByOrderByBusinessPlanAsc();
}
