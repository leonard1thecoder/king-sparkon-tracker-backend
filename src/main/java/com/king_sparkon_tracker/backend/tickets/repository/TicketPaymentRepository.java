package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketPaymentRepository extends JpaRepository<TicketPayment, String> {
    List<TicketPayment> findByEventIdIn(Collection<String> eventIds);
    Optional<TicketPayment> findByPaymentReference(String paymentReference);

    @Query("select coalesce(sum(payment.totalAmount), 0) from TicketPayment payment where payment.eventId in :eventIds and payment.status = :status")
    BigDecimal sumSuccessfulPaymentsByEventIds(@Param("eventIds") Collection<String> eventIds, @Param("status") TicketPaymentStatus status);
}
