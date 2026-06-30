package com.king_sparkon_tracker.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Query("""
			select s
			from Subscriber s
			where s.active = true
			  and (s.lastNotifiedAt is null or s.lastNotifiedAt < :cutoff)
			order by s.createdDate asc
			""")
	List<Subscriber> findDueActiveSubscribers(@Param("cutoff") OffsetDateTime cutoff, Pageable pageable);

	@Query("""
			select s
			from Subscriber s
			where s.active = true
			  and s.contactType = :contactType
			  and (s.lastNotifiedAt is null or s.lastNotifiedAt < :cutoff)
			order by s.createdDate asc
			""")
	List<Subscriber> findDueActiveSubscribersByContactType(
			@Param("contactType") SubscriberContactType contactType,
			@Param("cutoff") OffsetDateTime cutoff,
			Pageable pageable);

	@Query("""
			select s
			from Subscriber s
			where s.active = true
			  and s.subscriberType = :subscriberType
			  and s.affiliateRegistered = :affiliateRegistered
			  and (s.lastNotifiedAt is null or s.lastNotifiedAt < :cutoff)
			order by s.createdDate asc
			""")
	List<Subscriber> findDueActiveSubscribersBySubscriberTypeAndAffiliateRegistered(
			@Param("subscriberType") SubscriberType subscriberType,
			@Param("affiliateRegistered") boolean affiliateRegistered,
			@Param("cutoff") OffsetDateTime cutoff,
			Pageable pageable);

	@Query("""
			select s
			from Subscriber s
			where s.active = true
			  and s.affiliateRegistered = :affiliateRegistered
			  and (s.lastNotifiedAt is null or s.lastNotifiedAt < :cutoff)
			order by s.createdDate asc
			""")
	List<Subscriber> findDueActiveSubscribersByAffiliateRegistered(
			@Param("affiliateRegistered") boolean affiliateRegistered,
			@Param("cutoff") OffsetDateTime cutoff,
			Pageable pageable);

	@Query("""
			select s
			from Subscriber s
			where s.active = true
			  and s.preferredChannel = :preferredChannel
			  and (s.lastNotifiedAt is null or s.lastNotifiedAt < :cutoff)
			order by s.createdDate asc
			""")
	List<Subscriber> findDueActiveSubscribersByPreferredChannel(
			@Param("preferredChannel") PromotionChannel preferredChannel,
			@Param("cutoff") OffsetDateTime cutoff,
			Pageable pageable);
}
