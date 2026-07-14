package com.king_sparkon_tracker.backend.inventory.reservation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StockReservationRepository extends JpaRepository<StockReservation, String> {

	Optional<StockReservation> findByPaymentReferenceAndProduct_Id(String paymentReference, Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select reservation from StockReservation reservation where reservation.paymentReference = :paymentReference order by reservation.createdAt asc")
	List<StockReservation> findLockedByPaymentReference(@Param("paymentReference") String paymentReference);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select reservation from StockReservation reservation where reservation.transaction.id = :transactionId order by reservation.createdAt asc")
	List<StockReservation> findLockedByTransactionId(@Param("transactionId") Long transactionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select reservation from StockReservation reservation
			where reservation.status in :statuses and reservation.expiresAt <= :now
			order by reservation.expiresAt asc
			""")
	List<StockReservation> findExpiredLocked(
			@Param("statuses") Collection<StockReservationStatus> statuses,
			@Param("now") Instant now);
}
