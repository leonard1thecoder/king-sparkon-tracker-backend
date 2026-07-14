package com.king_sparkon_tracker.backend.tickets.reservation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, String> {
	Optional<TicketReservation> findByPayment_Id(String paymentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select reservation from TicketReservation reservation where reservation.payment.id = :paymentId")
	Optional<TicketReservation> findLockedByPaymentId(@Param("paymentId") String paymentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select reservation from TicketReservation reservation
			where reservation.status in :statuses and reservation.expiresAt <= :now
			order by reservation.expiresAt asc
			""")
	List<TicketReservation> findExpiredLocked(
			@Param("statuses") Collection<TicketReservationStatus> statuses,
			@Param("now") Instant now);
}
