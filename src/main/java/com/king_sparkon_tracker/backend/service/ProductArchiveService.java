package com.king_sparkon_tracker.backend.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@Service
@Transactional
public class ProductArchiveService {

	private final ProductRepository productRepository;
	private final TrackerUserService trackerUserService;
	private final BusinessAccessService businessAccessService;
	private final AuditLogService auditLogService;

	public ProductArchiveService(
			ProductRepository productRepository,
			TrackerUserService trackerUserService,
			BusinessAccessService businessAccessService,
			AuditLogService auditLogService) {
		this.productRepository = productRepository;
		this.trackerUserService = trackerUserService;
		this.businessAccessService = businessAccessService;
		this.auditLogService = auditLogService;
	}

	@CacheEvict(cacheNames = { "products", "tuckShopProducts", "productByBarcode" }, allEntries = true)
	public void archive(Long productId, String actorUsername) {
		if (productId == null || productId <= 0) {
			throw new IllegalArgumentException("Product id is required");
		}
		businessAccessService.requireFeature(actorUsername, BusinessFeature.CREATE_PRODUCTS);
		Business business = trackerUserService.businessForActor(actorUsername);
		Product product = productRepository.findWithBarcodesByIdAndBusiness_Id(productId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
		if (product.getStatus() == ProductStatus.ARCHIVED) {
			return;
		}
		product.setStatus(ProductStatus.ARCHIVED);
		productRepository.save(product);
		auditLogService.record(
				"PRODUCT_ARCHIVED",
				"Product",
				String.valueOf(productId),
				actorUsername,
				"Product removed from customer shop while transaction history was preserved",
				business);
	}
}
