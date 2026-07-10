package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.StripeWebhookEvent;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {

	boolean existsByStripeEventId(String stripeEventId);

	Optional<StripeWebhookEvent> findByStripeEventId(String stripeEventId);
}
