package com.king_sparkon_tracker.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.PayPalWebhookEvent;

public interface PayPalWebhookEventRepository extends JpaRepository<PayPalWebhookEvent, Long> {

	boolean existsByPaypalEventId(String paypalEventId);

	Optional<PayPalWebhookEvent> findByPaypalEventId(String paypalEventId);
}
