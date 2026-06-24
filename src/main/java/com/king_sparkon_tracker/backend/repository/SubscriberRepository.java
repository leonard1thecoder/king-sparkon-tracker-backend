package com.king_sparkon_tracker.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.PromotionChannel;
import com.king_sparkon_tracker.backend.model.Subscriber;
import com.king_sparkon_tracker.backend.model.SubscriberContactType;
import com.king_sparkon_tracker.backend.model.SubscriberType;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

	Optional<Subscriber> findByContactValue(String contactValue);

	long countByActiveTrue();

	long countByActiveTrueAndContactType(SubscriberContactType contactType);

	long countByActiveTrueAndSubscriberTypeAndAffiliateRegistered(SubscriberType subscriberType, boolean affiliateRegistered);

	long countByActiveTrueAndAffiliateRegistered(boolean affiliateRegistered);

	Page<Subscriber> findByActiveTrue(Pageable pageable);

	List<Subscriber> findTop200ByActiveTrueAndLastNotifiedAtIsNullOrActiveTrueAndLastNotifiedAtBeforeOrderByCreatedDateAsc(OffsetDateTime cutoff);

	List<Subscriber> findTop200ByActiveTrueAndContactTypeAndLastNotifiedAtIsNullOrActiveTrueAndContactTypeAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
			SubscriberContactType firstContactType,
			OffsetDateTime cutoff,
			SubscriberContactType secondContactType,
			OffsetDateTime secondCutoff);

	List<Subscriber> findTop200ByActiveTrueAndSubscriberTypeAndAffiliateRegisteredAndLastNotifiedAtIsNullOrActiveTrueAndSubscriberTypeAndAffiliateRegisteredAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
			SubscriberType firstSubscriberType,
			boolean firstAffiliateRegistered,
			OffsetDateTime cutoff,
			SubscriberType secondSubscriberType,
			boolean secondAffiliateRegistered,
			OffsetDateTime secondCutoff);

	List<Subscriber> findTop200ByActiveTrueAndAffiliateRegisteredAndLastNotifiedAtIsNullOrActiveTrueAndAffiliateRegisteredAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
			boolean firstAffiliateRegistered,
			OffsetDateTime cutoff,
			boolean secondAffiliateRegistered,
			OffsetDateTime secondCutoff);

	List<Subscriber> findTop200ByActiveTrueAndPreferredChannelAndLastNotifiedAtIsNullOrActiveTrueAndPreferredChannelAndLastNotifiedAtBeforeOrderByCreatedDateAsc(
			PromotionChannel firstPreferredChannel,
			OffsetDateTime cutoff,
			PromotionChannel secondPreferredChannel,
			OffsetDateTime secondCutoff);
}
