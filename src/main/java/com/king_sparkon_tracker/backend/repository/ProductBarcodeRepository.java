package com.king_sparkon_tracker.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.Business;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, Long> {

	@EntityGraph(attributePaths = { "product", "product.barcodes" })
	Optional<ProductBarcode> findByBarcode(String barcode);

	@EntityGraph(attributePaths = { "product", "product.barcodes" })
	@Query("select barcode from ProductBarcode barcode where barcode.barcode = :barcode and barcode.product.business.id = :businessId")
	Optional<ProductBarcode> findByBarcode(@Param("barcode") String barcode, @Param("businessId") Long businessId);

	@EntityGraph(attributePaths = "product")
	@Query("select barcode from ProductBarcode barcode where barcode.id = :id")
	Optional<ProductBarcode> findWithProductById(@Param("id") Long id);

	@EntityGraph(attributePaths = "product")
	@Query("select barcode from ProductBarcode barcode where barcode.id = :id and barcode.product.business.id = :businessId")
	Optional<ProductBarcode> findWithProductById(@Param("id") Long id, @Param("businessId") Long businessId);

	@EntityGraph(attributePaths = "product")
	@Query("select barcode from ProductBarcode barcode where barcode.referencee = :reference order by barcode.id asc")
	List<ProductBarcode> findByReference(@Param("reference") String reference);

	@EntityGraph(attributePaths = "product")
	@Query("""
			select barcode
			from ProductBarcode barcode
			where barcode.referencee = :reference
			and barcode.product.business.id = :businessId
			order by barcode.id asc
			""")
	List<ProductBarcode> findByReference(@Param("reference") String reference, @Param("businessId") Long businessId);

	List<ProductBarcode> findByBarcodeIn(List<String> barcodes);

	List<ProductBarcode> findByProduct_IdOrderByIdAsc(Long productId);

	List<ProductBarcode> findByStatus(ProductBarcodeStatus status);

	boolean existsByBarcode(String barcode);

	long countByProduct_Id(Long productId);

	long countByStatus(ProductBarcodeStatus status);

	@Query("""
			select barcode.product.business as business, count(barcode) as expiredCount
			from ProductBarcode barcode
			where barcode.status = :status
			and barcode.product.returnableEnabled = true
			group by barcode.product.business
			""")
	List<ReturnableBarcodeExpiryDigest> countReturnableExpiryDigestByBusiness(
			@Param("status") ProductBarcodeStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update ProductBarcode barcode set barcode.status = :status where barcode.id = :id")
	int updateStatus(@Param("id") Long id, @Param("status") ProductBarcodeStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update ProductBarcode barcode
			set barcode.status = :newStatus
			where barcode.status = :oldStatus
			""")
	int updateStatusForAllMatchingStatus(
			@Param("oldStatus") ProductBarcodeStatus oldStatus,
			@Param("newStatus") ProductBarcodeStatus newStatus);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update ProductBarcode barcode
			set barcode.status = :newStatus
			where barcode.status = :oldStatus
			and barcode.product.business.id = :businessId
			""")
	int updateStatusForAllMatchingStatus(
			@Param("businessId") Long businessId,
			@Param("oldStatus") ProductBarcodeStatus oldStatus,
			@Param("newStatus") ProductBarcodeStatus newStatus);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update ProductBarcode barcode
			set barcode.status = :newStatus
			where barcode.status = :oldStatus
			and barcode.product.returnableEnabled = true
			""")
	int updateStatusForReturnableProducts(
			@Param("oldStatus") ProductBarcodeStatus oldStatus,
			@Param("newStatus") ProductBarcodeStatus newStatus);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update ProductBarcode barcode
			set barcode.status = :newStatus
			where barcode.status = :oldStatus
			and barcode.product.business.id = :businessId
			and barcode.product.returnableEnabled = true
			""")
	int updateStatusForReturnableProducts(
			@Param("businessId") Long businessId,
			@Param("oldStatus") ProductBarcodeStatus oldStatus,
			@Param("newStatus") ProductBarcodeStatus newStatus);

	interface ReturnableBarcodeExpiryDigest {
		Business getBusiness();

		long getExpiredCount();
	}
}
