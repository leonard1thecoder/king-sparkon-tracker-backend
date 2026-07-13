package com.king_sparkon_tracker.backend.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.ProductPromotion;

public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {

	@EntityGraph(attributePaths = { "product", "product.business", "product.barcodes", "business" })
	List<ProductPromotion> findByBusiness_IdOrderByCreatedAtDesc(Long businessId);

	@EntityGraph(attributePaths = { "product", "product.business", "product.barcodes", "business" })
	List<ProductPromotion> findByActiveTrueAndStartsAtLessThanEqualAndEndsAtAfterOrderByCreatedAtDesc(
			OffsetDateTime startsAt,
			OffsetDateTime endsAt,
			Pageable pageable);

	List<ProductPromotion> findByProduct_IdAndActiveTrue(Long productId);
}
