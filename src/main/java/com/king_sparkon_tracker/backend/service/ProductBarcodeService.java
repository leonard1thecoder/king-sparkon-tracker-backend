package com.king_sparkon_tracker.backend.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository.ReturnableBarcodeExpiryDigest;

@Service
@Transactional
public class ProductBarcodeService {

	private static final Logger log = LoggerFactory.getLogger(ProductBarcodeService.class);
	private static final LocalTime RETURNABLE_DISABLE_START = LocalTime.of(17, 0);

	private final ProductBarcodeRepository productBarcodeRepository;
	private final AuditLogService auditLogService;
	private final Clock clock;
	private final TrackerUserService userService;
	private final AppEmailService appEmailService;

	public ProductBarcodeService(
			ProductBarcodeRepository productBarcodeRepository,
			AuditLogService auditLogService,
			Clock clock,
			TrackerUserService userService,
			AppEmailService appEmailService) {
		this.productBarcodeRepository = productBarcodeRepository;
		this.auditLogService = auditLogService;
		this.clock = clock;
		this.userService = userService;
		this.appEmailService = appEmailService;
	}

	/**
	 * Returns barcode claim rows by customer reference email, expiring returnable claims first when the weekly cutoff is active.
	 */
	public List<ProductBarcode> findByReference(String reference, String actorUsername) {
		String normalizedReference = EmailAddressNormalizer.normalizeRequired(
				reference,
				"Reference email must be a valid email address");
		Business business = userService.businessForActor(actorUsername);
		expireReturnableBarcodesIfNeeded(actorUsername);
		List<ProductBarcode> barcodes = productBarcodeRepository.findByReference(normalizedReference, business.getId());
		if (barcodes.isEmpty()) {
			throw new ResourceNotFoundException("Barcode not found for reference: " + normalizedReference);
		}
		log.debug(
				"product_barcode_reference_lookup reference={} businessId={} resultCount={} actor={}",
				normalizedReference,
				business.getId(),
				barcodes.size(),
				actorUsername);
		return barcodes;
	}

	/**
	 * Claims one unambiguous returnable barcode by customer reference email.
	 */
	public ProductBarcode claimByReference(String reference, String actorUsername) {
		List<ProductBarcode> barcodes = findByReference(reference, actorUsername);
		List<ProductBarcode> activeReturnableBarcodes = barcodes.stream()
				.filter(barcode -> barcode.getProduct().isReturnableEnabled())
				.filter(barcode -> barcode.getStatus() == ProductBarcodeStatus.NOT_CLAIMED)
				.toList();
		if (activeReturnableBarcodes.size() > 1) {
			throw new IllegalArgumentException("Multiple not claimed barcodes found for reference; claim by barcode id");
		}
		if (activeReturnableBarcodes.isEmpty()) {
			ProductBarcode barcode = barcodes.stream()
					.filter(candidate -> candidate.getProduct().isReturnableEnabled())
					.findFirst()
					.orElse(barcodes.getFirst());
			validateClaimable(barcode);
		}
		return claim(activeReturnableBarcodes.getFirst(), actorUsername);
	}

	/**
	 * Claims a specific barcode row after a reference search table selects it.
	 */
	public ProductBarcode claimById(Long id, String actorUsername) {
		Long barcodeId = requirePresent(id, "Barcode id is required");
		Business business = userService.businessForActor(actorUsername);
		expireReturnableBarcodesIfNeeded(actorUsername);
		ProductBarcode barcode = productBarcodeRepository.findWithProductById(barcodeId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + barcodeId));
		validateClaimable(barcode);
		return claim(barcode, actorUsername);
	}

	/**
	 * Scheduled weekly cutoff: all unclaimed returnable barcode claims expire at Friday 17:00 in business time.
	 */
	@Scheduled(cron = "0 0 17 * * FRI", zone = "Africa/Johannesburg")
	public void expireReturnableBarcodesAtFridayCutoff() {
		expireReturnableBarcodesIfNeeded("system");
	}

	public int expireReturnableBarcodesIfNeeded(String actorUsername) {
		LocalDateTime now = LocalDateTime.now(clock);
		if (!isReturnableClaimDisabled(now)) {
			return 0;
		}
		Business business = "system".equals(actorUsername) ? null : userService.businessForActor(actorUsername);
		List<ReturnableBarcodeExpiryDigest> systemDigest = business == null
				? productBarcodeRepository.countReturnableExpiryDigestByBusiness(ProductBarcodeStatus.NOT_CLAIMED)
				: List.of();
		int expired = business == null
				? productBarcodeRepository.updateStatusForReturnableProducts(
						ProductBarcodeStatus.NOT_CLAIMED,
						ProductBarcodeStatus.EXPIRED)
				: productBarcodeRepository.updateStatusForReturnableProducts(
						business.getId(),
						ProductBarcodeStatus.NOT_CLAIMED,
						ProductBarcodeStatus.EXPIRED);
		if (expired > 0) {
			auditLogService.record(
					"PRODUCT_BARCODE_RETURNABLE_EXPIRED",
					"ProductBarcode",
					"returnable",
					actorUsername,
					"Expired not claimed returnable barcodes: " + expired,
					business);
			log.info(
					"product_barcode_returnable_expired expiredCount={} businessId={} actor={}",
					expired,
					business == null ? null : business.getId(),
					actorUsername);
			if (business == null) {
				sendSystemReturnableExpiredDigest(systemDigest, actorUsername);
			} else {
				sendReturnableExpiredDigest(business, expired, actorUsername);
			}
		}
		return expired;
	}

	private ProductBarcode claim(ProductBarcode barcode, String actorUsername) {
		validateClaimable(barcode);
		int updated = productBarcodeRepository.updateStatus(barcode.getId(), ProductBarcodeStatus.CLAIMED);
		if (updated != 1) {
			throw new ResourceNotFoundException("Barcode not found: " + barcode.getId());
		}
		barcode.setStatus(ProductBarcodeStatus.CLAIMED);
		auditLogService.record(
				"PRODUCT_BARCODE_CLAIMED",
				"ProductBarcode",
				String.valueOf(barcode.getId()),
				actorUsername,
				"Returnable barcode claimed by reference: " + barcode.getReferenceEmail(),
				barcode.getProduct().getBusiness());
		log.info(
				"product_barcode_claimed barcodeId={} businessId={} barcode={} reference={} actor={}",
				barcode.getId(),
				barcode.getProduct().getBusiness() == null ? null : barcode.getProduct().getBusiness().getId(),
				barcode.getBarcode(),
				barcode.getReferenceEmail(),
				actorUsername);
		sendBarcodeClaimedNotification(barcode, actorUsername);
		return barcode;
	}

	private void validateClaimable(ProductBarcode barcode) {
		if (!barcode.getProduct().isReturnableEnabled()) {
			throw new IllegalArgumentException("Only returnable barcodes can be claimed");
		}
		if (barcode.getStatus() == ProductBarcodeStatus.CLAIMED) {
			throw new IllegalArgumentException("Returnable barcode already claimed");
		}
		if (barcode.getStatus() == ProductBarcodeStatus.EXPIRED) {
			throw new IllegalArgumentException("Returnable barcode claim has expired");
		}
	}

	private boolean isReturnableClaimDisabled(LocalDateTime dateTime) {
		DayOfWeek day = dateTime.getDayOfWeek();
		if (day == DayOfWeek.FRIDAY) {
			return !dateTime.toLocalTime().isBefore(RETURNABLE_DISABLE_START);
		}
		return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private <T> T requirePresent(T value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

	private void sendBarcodeClaimedNotification(ProductBarcode barcode, String actorUsername) {
		Business business = barcode.getProduct().getBusiness();
		try {
			appEmailService.sendBarcodeClaimedEmail(barcode, actorUsername);
		} catch (RuntimeException exception) {
			log.warn(
					"barcode_claimed_email_failed_non_blocking recipient={} businessId={} barcodeId={} actor={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					barcode.getId(),
					actorUsername,
					exception.getMessage());
		}
	}

	private void sendSystemReturnableExpiredDigest(
			List<ReturnableBarcodeExpiryDigest> digest,
			String actorUsername) {
		for (ReturnableBarcodeExpiryDigest entry : digest) {
			sendReturnableExpiredDigest(entry.getBusiness(), entry.getExpiredCount(), actorUsername);
		}
	}

	private void sendReturnableExpiredDigest(Business business, long expiredCount, String actorUsername) {
		try {
			appEmailService.sendReturnableBarcodesExpiredEmail(business, expiredCount, actorUsername);
		} catch (RuntimeException exception) {
			log.warn(
					"returnable_barcodes_expired_email_failed_non_blocking recipient={} businessId={} expiredCount={} actor={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					expiredCount,
					actorUsername,
					exception.getMessage());
		}
	}
}
