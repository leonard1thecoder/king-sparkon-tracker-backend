package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.king_sparkon_tracker.backend.model.ProductBarcodeConfiguration;

public interface ProductBarcodeConfigurationRepository extends JpaRepository<ProductBarcodeConfiguration, Long> {

	@EntityGraph(attributePaths = { "product", "product.business" })
	List<ProductBarcodeConfiguration> findByProduct_Business_IdOrderByProduct_IdAsc(Long businessId);
}
