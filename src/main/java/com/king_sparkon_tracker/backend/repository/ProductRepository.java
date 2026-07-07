package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Override
	@EntityGraph(attributePaths = "barcodes")
	Page<Product> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "barcodes")
	@Query("select product from Product product where product.id = :id")
	Optional<Product> findWithBarcodesById(@Param("id") Long id);

	@EntityGraph(attributePaths = "barcodes")
	Page<Product> findByBusiness_Id(Long businessId, Pageable pageable);

	@EntityGraph(attributePaths = "barcodes")
	@Query("select product from Product product where product.id = :id and product.business.id = :businessId")
	Optional<Product> findWithBarcodesByIdAndBusiness_Id(@Param("id") Long id, @Param("businessId") Long businessId);

	@EntityGraph(attributePaths = "barcodes")
	Optional<Product> findFirstByProductBarcode(String productBarcode);

	@EntityGraph(attributePaths = "barcodes")
	Optional<Product> findFirstByProductBarcodeAndBusiness_Id(String productBarcode, Long businessId);

	boolean existsByBusiness_IdAndProductBarcode(Long businessId, String productBarcode);

	@EntityGraph(attributePaths = "barcodes")
	@Query("""
			select product
			from Product product
			where product.status = :status
				and product.stockQuantity > 0
				and (:businessId is null or product.business.id = :businessId)
				and (:category is null or product.category = :category)
			""")
	Page<Product> findTuckShopProducts(
			@Param("status") ProductStatus status,
			@Param("businessId") Long businessId,
			@Param("category") ProductCategory category,
			Pageable pageable);

	@EntityGraph(attributePaths = "barcodes")
	@Query("""
			select product
			from Product product
			where product.status = :status
				and product.stockQuantity > 0
				and (:businessId is null or product.business.id = :businessId)
				and (:category is null or product.category = :category)
				and (
					lower(product.name) like lower(concat('%', :search, '%'))
					or lower(product.productBarcode) like lower(concat('%', :search, '%'))
					or lower(product.business.name) like lower(concat('%', :search, '%'))
				)
			""")
	Page<Product> searchTuckShopProducts(
			@Param("status") ProductStatus status,
			@Param("businessId") Long businessId,
			@Param("category") ProductCategory category,
			@Param("search") String search,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select product from Product product where product.id = :id")
	Optional<Product> findLockedById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select product from Product product where product.id = :id and product.business.id = :businessId")
	Optional<Product> findLockedByIdAndBusiness_Id(@Param("id") Long id, @Param("businessId") Long businessId);

	List<Product> findByBusiness_Id(Long businessId);

	long countByCategory(ProductCategory category);

	long countByStockQuantityLessThanEqual(int stockQuantity);

	long countByBusiness_IdAndCategory(Long businessId, ProductCategory category);

	long countByBusiness_IdAndStockQuantityLessThanEqual(Long businessId, int stockQuantity);
}
