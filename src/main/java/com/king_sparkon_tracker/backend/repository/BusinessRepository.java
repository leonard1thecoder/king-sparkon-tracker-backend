package com.king_sparkon_tracker.backend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessStatus;

public interface BusinessRepository extends JpaRepository<Business, Long> {

	Optional<Business> findByOwner_Username(String username);

	List<Business> findByBusinessStatusIn(Collection<BusinessStatus> statuses);

	List<Business> findByBusinessStatusAndTrialEndDateBefore(BusinessStatus status, LocalDateTime dateTime);

	List<Business> findByBusinessStatusAndCurrentBillingPeriodEndDateBefore(BusinessStatus status, LocalDateTime dateTime);
}
