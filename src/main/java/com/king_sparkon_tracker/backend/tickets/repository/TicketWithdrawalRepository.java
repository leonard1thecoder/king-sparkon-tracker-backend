package com.king_sparkon_tracker.backend.tickets.repository;

import com.king_sparkon_tracker.backend.tickets.model.TicketWithdrawal;
import com.king_sparkon_tracker.backend.tickets.model.TicketWithdrawalStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketWithdrawalRepository extends JpaRepository<TicketWithdrawal, String> {
    List<TicketWithdrawal> findByOwnerIdOrderByRequestedAtDesc(String ownerId);

    @Query("select coalesce(sum(withdrawal.grossAmount), 0) from TicketWithdrawal withdrawal where withdrawal.ownerId = :ownerId and withdrawal.status in :statuses")
    BigDecimal sumGrossByOwnerIdAndStatuses(@Param("ownerId") String ownerId, @Param("statuses") Collection<TicketWithdrawalStatus> statuses);
}
