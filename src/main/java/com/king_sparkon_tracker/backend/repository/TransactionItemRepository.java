package com.king_sparkon_tracker.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TransactionItem;

public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {

	@EntityGraph(attributePaths = { "transaction", "product" })
	List<TransactionItem> findByTransaction_DateBetweenAndProduct_Category(
			LocalDateTime from,
			LocalDateTime to,
			ProductCategory category);

	@EntityGraph(attributePaths = { "transaction", "product" })
	List<TransactionItem> findByTransaction_Business_IdAndTransaction_DateBetweenAndProduct_Category(
			Long businessId,
			LocalDateTime from,
			LocalDateTime to,
			ProductCategory category);

	@EntityGraph(attributePaths = { "transaction", "product" })
	List<TransactionItem> findByTransaction_DateBetween(LocalDateTime from, LocalDateTime to);

	@EntityGraph(attributePaths = { "transaction", "product" })
	List<TransactionItem> findByTransaction_Business_IdAndTransaction_DateBetween(
			Long businessId,
			LocalDateTime from,
			LocalDateTime to);
}
