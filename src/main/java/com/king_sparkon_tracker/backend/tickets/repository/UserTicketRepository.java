package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.model.UserTicket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTicketRepository extends JpaRepository<UserTicket, String> {
    List<UserTicket> findByUserIdOrderByPurchasedAtDesc(String userId);
    Optional<UserTicket> findByQrCodeValue(String qrCodeValue);
    Optional<UserTicket> findByTicketReferenceIgnoreCase(String ticketReference);
    long countByEventId(String eventId);
    long countByTicketType(TicketType ticketType);

    @Query("select coalesce(sum(ticket.pricePaid), 0) from UserTicket ticket where ticket.eventId in :eventIds")
    java.math.BigDecimal sumTicketRevenueByEventIds(@Param("eventIds") List<String> eventIds);
}
