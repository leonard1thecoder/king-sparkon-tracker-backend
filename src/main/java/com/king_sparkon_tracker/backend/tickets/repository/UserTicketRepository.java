package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.model.UserTicket;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTicketRepository extends JpaRepository<UserTicket, String> {
    List<UserTicket> findByUserIdOrderByPurchasedAtDesc(String userId);
    List<UserTicket> findByPaymentIdOrderByPurchasedAtAsc(String paymentId);
    Optional<UserTicket> findByQrCodeValue(String qrCodeValue);
    Optional<UserTicket> findByTicketReferenceIgnoreCase(String ticketReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from UserTicket ticket where ticket.qrCodeValue = :qrCodeValue")
    Optional<UserTicket> findLockedByQrCodeValue(@Param("qrCodeValue") String qrCodeValue);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from UserTicket ticket where lower(ticket.ticketReference) = lower(:ticketReference)")
    Optional<UserTicket> findLockedByTicketReferenceIgnoreCase(@Param("ticketReference") String ticketReference);

    long countByEventId(String eventId);
    long countByTicketType(TicketType ticketType);

    @Query("select coalesce(sum(ticket.pricePaid), 0) from UserTicket ticket where ticket.eventId in :eventIds and ticket.status in ('ACTIVE', 'USED')")
    java.math.BigDecimal sumTicketRevenueByEventIds(@Param("eventIds") List<String> eventIds);
}
