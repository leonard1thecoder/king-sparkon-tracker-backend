package com.king_sparkon_tracker.backend.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.king_sparkon_tracker.backend.model.BusinessAccountEntryStatus;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessAccountLedgerEntry;

import jakarta.persistence.LockModeType;

public interface BusinessAccountLedgerEntryRepository extends JpaRepository<BusinessAccountLedgerEntry, Long> {

	List<BusinessAccountLedgerEntry> findByBusiness_IdOrderByCreatedDateDesc(Long businessId);

	List<BusinessAccountLedgerEntry> findByBusiness_IdAndEntryTypeOrderByCreatedDateDesc(
			Long businessId,
			BusinessAccountEntryType entryType);

	Optional<BusinessAccountLedgerEntry> findByBusiness_IdAndEntryTypeAndProviderReference(
			Long businessId,
			BusinessAccountEntryType entryType,
			String providerReference);

	boolean existsByBusiness_IdAndEntryTypeAndProviderReference(
			Long businessId,
			BusinessAccountEntryType entryType,
			String providerReference);

	@Query("select coalesce(sum(entry.amount), 0) from BusinessAccountLedgerEntry entry where entry.business.id = :businessId and entry.status = :status")
	BigDecimal sumAmountByBusinessIdAndStatus(
			@Param("businessId") Long businessId,
			@Param("status") BusinessAccountEntryStatus status);

	@Query("select coalesce(sum(entry.amount), 0) from BusinessAccountLedgerEntry entry where entry.business.id = :businessId and entry.status = :status and entry.entryType in :entryTypes")
	BigDecimal sumAmountByBusinessIdAndStatusAndEntryTypeIn(
			@Param("businessId") Long businessId,
			@Param("status") BusinessAccountEntryStatus status,
			@Param("entryTypes") Collection<BusinessAccountEntryType> entryTypes);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select entry from BusinessAccountLedgerEntry entry where entry.business.id = :businessId and entry.status = :status order by entry.createdDate asc")
	List<BusinessAccountLedgerEntry> lockPostedEntries(
			@Param("businessId") Long businessId,
			@Param("status") BusinessAccountEntryStatus status);
}
