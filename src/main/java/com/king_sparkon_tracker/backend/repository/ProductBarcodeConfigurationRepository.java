package com.king_sparkon_tracker.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.king_sparkon_tracker.backend.model.ProductBarcodeConfiguration;

public interface ProductBarcodeConfigurationRepository extends JpaRepository<ProductBarcodeConfiguration, Long> {

@EntityGraph(attributePaths = "product")
Optional<ProductBarcodeConfiguration> findByProduct_Id(Long productId);
	
	@EntityGraph(attributePaths = { "product", "product.business" })
	List<ProductBarcodeConfiguration> findByProduct_Business_IdOrderByProduct_IdAsc(Long businessId);
}
