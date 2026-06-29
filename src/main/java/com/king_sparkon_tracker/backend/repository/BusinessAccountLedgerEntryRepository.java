package com.king_sparkon_tracker.backend.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.king_sparkon_tracker.backend.model.BusinessAccountEntryStatus;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;

import jakarta.persistence.LockModeType;

public interface BusinessAccountLedgerEntryRepository extends JpaRepository<BusinessAccountLedgerEntry, Long> {

	List<BusinessAccountLedgerEntry> findByBusiness_IdOrderByCreatedDateDesc(Long businessId);

	@Query("select coalesce(sum(entry.amount), 0) from BusinessAccountLedgerEntry entry where entry.business.id = :businessId and entry.status = :status")
	BigDecimal sumAmountByBusinessIdAndStatus(@Param("businessId") Long businessId, @Param("status") BusinessAccountEntryStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select entry from BusinessAccountLedgerEntry entry where entry.business.id = :businessId and entry.status = :status order by entry.createdDate asc")
	List<BusinessAccountLedgerEntry> lockPostedEntries(@Param("businessId") Long businessId, @Param("status") BusinessAccountEntryStatus status);
}
