package com.king_sparkon_tracker.backend.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;

@Service
@Transactional
public class ProductBarcodeClaimService {

	private static final Logger log = LoggerFactory.getLogger(ProductBarcodeClaimService.class);

	private final ProductBarcodeRepository productBarcodeRepository;
	private final TrackerUserService userService;
	private final BusinessAccessService businessAccessService;
	private final Clock clock;

	public ProductBarcodeClaimService(
			ProductBarcodeRepository productBarcodeRepository,
			TrackerUserService userService,
			BusinessAccessService businessAccessService,
			Clock clock) {
		this.productBarcodeRepository = productBarcodeRepository;
		this.userService = userService;
		this.businessAccessService = businessAccessService;
		this.clock = clock;
	}

	@Scheduled(cron = "0 0 17 * * FRI", zone = "Africa/Johannesburg")
	public void expireUnclaimedBarcodesAtFridayCutoff() {
		int expiredCount = expireUnclaimedBarcodes();

		log.info(
				"product_barcode_claims_friday_expiry completedAt={} expiredCount={}",
				LocalDateTime.now(clock),
				expiredCount
		);
	}

	public int expireUnclaimedBarcodes() {
		return productBarcodeRepository.updateStatusForAllMatchingStatus(
				ProductBarcodeStatus.NOT_CLAIMED,
				ProductBarcodeStatus.EXPIRED
		);
	}

	public ProductBarcode claimBarcode(Long barcodeId, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.SCAN_BARCODES);

		if (barcodeId == null) {
			throw new IllegalArgumentException("Barcode id is required");
		}

		Business business = userService.businessForActor(actorUsername);

		ProductBarcode barcode = productBarcodeRepository.findWithProductById(barcodeId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product barcode not found: " + barcodeId));

		if (barcode.getStatus() == ProductBarcodeStatus.NOT_CLAIMABLE) {
			throw new IllegalArgumentException("This barcode belongs to a non-returnable product and cannot be claimed");
		}

		if (barcode.getStatus() == ProductBarcodeStatus.EXPIRED) {
			throw new IllegalArgumentException("This barcode claim has expired");
		}

		if (barcode.getStatus() == ProductBarcodeStatus.CLAIMED) {
			throw new IllegalArgumentException("This barcode was already claimed");
		}

		barcode.setStatus(ProductBarcodeStatus.CLAIMED);
		return productBarcodeRepository.save(barcode);
	}
}
