package com.king_sparkon_tracker.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

	@Override
	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	List<InventoryTransaction> findAll();

	@Override
	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Page<InventoryTransaction> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Page<InventoryTransaction> findByBusiness_Id(Long businessId, Pageable pageable);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Page<InventoryTransaction> findByEmployee_IdAndBusiness_Id(Long employeeId, Long businessId, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Optional<InventoryTransaction> findById(Long id);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Optional<InventoryTransaction> findByIdAndBusiness_Id(Long id, Long businessId);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	List<InventoryTransaction> findByBusiness_IdAndTypeAndPaymentTypeAndPaymentStatusAndTransactionWithdrawalIdIsNullAndDateLessThanEqualOrderByDateAsc(
			Long businessId,
			TransactionType type,
			TransactionPaymentType paymentType,
			TransactionPaymentStatus paymentStatus,
			LocalDateTime availableBefore);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	@Query("""
			select it
			from InventoryTransaction it
			where it.business.id = :businessId
				and it.id in :transactionIds
				and it.type = :type
				and it.paymentType = :paymentType
				and it.paymentStatus = :paymentStatus
				and it.transactionWithdrawalId is null
				and it.date <= :availableBefore
			order by it.date asc
			""")
	List<InventoryTransaction> findEligibleWebsitePaymentWithdrawalsByIds(
			@Param("businessId") Long businessId,
			@Param("transactionIds") List<Long> transactionIds,
			@Param("type") TransactionType type,
			@Param("paymentType") TransactionPaymentType paymentType,
			@Param("paymentStatus") TransactionPaymentStatus paymentStatus,
			@Param("availableBefore") LocalDateTime availableBefore);
}
