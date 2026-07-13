package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessAccountEntryType;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductPromotion;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.repository.ProductPromotionRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@Service
@Transactional
public class ProductPromotionService {

	private final ProductPromotionRepository promotionRepository;
	private final ProductRepository productRepository;
	private final TrackerUserService trackerUserService;
	private final BusinessAccessService businessAccessService;
	private final BusinessAccountService businessAccountService;
	private final AuditLogService auditLogService;
	private final BigDecimal promotionPrice;
	private final int promotionDays;

	public ProductPromotionService(
			ProductPromotionRepository promotionRepository,
			ProductRepository productRepository,
			TrackerUserService trackerUserService,
			BusinessAccessService businessAccessService,
			BusinessAccountService businessAccountService,
			AuditLogService auditLogService,
			@Value("${app.products.promotion-price-zar:100.00}") BigDecimal promotionPrice,
			@Value("${app.products.promotion-duration-days:7}") int promotionDays) {
		this.promotionRepository = promotionRepository;
		this.productRepository = productRepository;
		this.trackerUserService = trackerUserService;
		this.businessAccessService = businessAccessService;
		this.businessAccountService = businessAccountService;
		this.auditLogService = auditLogService;
		this.promotionPrice = money(promotionPrice);
		this.promotionDays = Math.max(promotionDays, 1);
	}

	public ProductPromotion promote(Long productId, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.CREATE_PRODUCTS);
		Business business = trackerUserService.businessForActor(actorUsername);
		Product product = productRepository.findLockedByIdAndBusiness_Id(productId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

		if (product.getStatus() != ProductStatus.CREATED) {
			throw new IllegalArgumentException("Only active products can be promoted");
		}
		if (product.getStockQuantity() <= 0) {
			throw new IllegalArgumentException("Product must have stock before it can be promoted");
		}

		var ledgerEntry = businessAccountService.debitPromotion(
				business,
				promotionPrice,
				BusinessAccountEntryType.PROMOTION_DEBIT,
				"Promoted product: " + product.getName(),
				actorUsername);

		promotionRepository.findByProduct_IdAndActiveTrue(productId).forEach(ProductPromotion::deactivate);

		OffsetDateTime startsAt = OffsetDateTime.now();
		ProductPromotion promotion = promotionRepository.save(new ProductPromotion(
				product,
				business,
				promotionPrice,
				ledgerEntry.getId(),
				startsAt,
				startsAt.plusDays(promotionDays),
				actorUsername));

		auditLogService.record(
				"PRODUCT_PROMOTED",
				"Product",
				String.valueOf(productId),
				actorUsername,
				"Product promoted for " + promotionDays + " days at " + promotionPrice + " ZAR",
				business);
		return promotion;
	}

	@Transactional(readOnly = true)
	public List<ProductPromotion> ownerPromotions(String actorUsername) {
		Business business = trackerUserService.businessForActor(actorUsername);
		return promotionRepository.findByBusiness_IdOrderByCreatedAtDesc(business.getId());
	}

	@Transactional(readOnly = true)
	public List<ProductPromotion> activePromotions(int limit) {
		OffsetDateTime now = OffsetDateTime.now();
		int safeLimit = Math.min(Math.max(limit, 1), 24);
		return promotionRepository.findByActiveTrueAndStartsAtLessThanEqualAndEndsAtAfterOrderByCreatedAtDesc(
				now,
				now,
				PageRequest.of(0, safeLimit));
	}

	public BigDecimal promotionPrice() {
		return promotionPrice;
	}

	public int promotionDays() {
		return promotionDays;
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
	}
}
