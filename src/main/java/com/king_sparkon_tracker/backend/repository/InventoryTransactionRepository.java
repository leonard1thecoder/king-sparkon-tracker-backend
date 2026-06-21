package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.InventoryTransaction;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

	@Override
	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	List<InventoryTransaction> findAll();

	@Override
	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Page<InventoryTransaction> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Page<InventoryTransaction> findByBusiness_Id(Long businessId, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Optional<InventoryTransaction> findById(Long id);

	@EntityGraph(attributePaths = { "employee", "owner", "items", "items.product", "items.barcodes" })
	Optional<InventoryTransaction> findByIdAndBusiness_Id(Long id, Long businessId);
}
