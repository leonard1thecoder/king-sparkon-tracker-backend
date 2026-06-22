package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.BusinessSubscription;
import com.king_sparkon_tracker.backend.model.SubscriptionPaymentStatus;

public interface BusinessSubscriptionRepository extends JpaRepository<BusinessSubscription, Long> {

	List<BusinessSubscription> findByBusiness_IdOrderByCreatedDateDesc(Long businessId);

	Optional<BusinessSubscription> findTopByBusiness_IdOrderByCreatedDateDesc(Long businessId);

	Optional<BusinessSubscription> findByPaypalSubscriptionId(String paypalSubscriptionId);

	Optional<BusinessSubscription> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

	Optional<BusinessSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

	List<BusinessSubscription> findByStatus(SubscriptionPaymentStatus status);
}
