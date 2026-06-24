package com.king_sparkon_tracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.PromotionNotification;

public interface PromotionNotificationRepository extends JpaRepository<PromotionNotification, Long> {

	boolean existsByPromotion_IdAndSubscriber_Id(Long promotionId, Long subscriberId);
}
