package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;

@ExtendWith(MockitoExtension.class)
class ProductBarcodeServiceTest {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Johannesburg");

	@Mock
	private ProductBarcodeRepository productBarcodeRepository;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private TrackerUserService userService;

	@Mock
	private AppEmailService appEmailService;

	private Business business;

	@BeforeEach
	void setUp() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new com.king_sparkon_tracker.backend.model.Privilege(com.king_sparkon_tracker.backend.model.PrivilegeRole.Owner));
		business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		lenient().when(userService.businessForActor("worker")).thenReturn(business);
	}

	@Test
	void findByReferenceExpiresNotClaimedReturnableBarcodesAtFridayCutoff() {
		ProductBarcode barcode = barcode(14L, true, ProductBarcodeStatus.EXPIRED);
		when(productBarcodeRepository.updateStatusForReturnableProducts(
				1L,
				ProductBarcodeStatus.NOT_CLAIMED,
				ProductBarcodeStatus.EXPIRED))
				.thenReturn(3);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(barcode));

		List<ProductBarcode> result = serviceAt(LocalDateTime.of(2026, 6, 5, 17, 0))
				.findByReference(" customer@example.com ", "worker");

		assertThat(result).containsExactly(barcode);
		verify(productBarcodeRepository).updateStatusForReturnableProducts(
				1L,
				ProductBarcodeStatus.NOT_CLAIMED,
				ProductBarcodeStatus.EXPIRED);
		verify(auditLogService).record(
				"PRODUCT_BARCODE_RETURNABLE_EXPIRED",
				"ProductBarcode",
				"returnable",
				"worker",
				"Expired not claimed returnable barcodes: 3",
				business);
		verify(appEmailService).sendReturnableBarcodesExpiredEmail(business, 3, "worker");
	}

	@Test
	void findByReferenceDoesNotExpireBeforeFridayCutoff() {
		ProductBarcode barcode = barcode(14L, true, ProductBarcodeStatus.NOT_CLAIMED);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(barcode));

		List<ProductBarcode> result = serviceAt(LocalDateTime.of(2026, 6, 5, 16, 59))
				.findByReference("customer@example.com", "worker");

		assertThat(result).containsExactly(barcode);
		verify(productBarcodeRepository, never()).updateStatusForReturnableProducts(
				1L,
				ProductBarcodeStatus.NOT_CLAIMED,
				ProductBarcodeStatus.EXPIRED);
	}

	@Test
	void findByReferenceRejectsCellphoneReference() {
		assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.findByReference("0821234567", "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Reference email must be a valid email address");
	}

	@Test
	void claimByReferenceMarksSingleNotClaimedReturnableBarcodeAsClaimed() {
		ProductBarcode barcode = barcode(14L, true, ProductBarcodeStatus.NOT_CLAIMED);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(barcode));
		when(productBarcodeRepository.updateStatus(14L, ProductBarcodeStatus.CLAIMED)).thenReturn(1);

		ProductBarcode result = serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.claimByReference("customer@example.com", "worker");

		assertThat(result.getStatus()).isEqualTo(ProductBarcodeStatus.CLAIMED);
		verify(productBarcodeRepository).updateStatus(14L, ProductBarcodeStatus.CLAIMED);
		verify(auditLogService).record(
				"PRODUCT_BARCODE_CLAIMED",
				"ProductBarcode",
				"14",
				"worker",
				"Returnable barcode claimed by reference: customer@example.com",
				business);
		verify(appEmailService).sendBarcodeClaimedEmail(barcode, "worker");
	}

	@Test
	void claimByReferenceContinuesWhenClaimEmailFails() {
		ProductBarcode barcode = barcode(14L, true, ProductBarcodeStatus.NOT_CLAIMED);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(barcode));
		when(productBarcodeRepository.updateStatus(14L, ProductBarcodeStatus.CLAIMED)).thenReturn(1);
		when(appEmailService.sendBarcodeClaimedEmail(barcode, "worker"))
				.thenThrow(new IllegalStateException("smtp down"));

		ProductBarcode result = serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.claimByReference("customer@example.com", "worker");

		assertThat(result.getStatus()).isEqualTo(ProductBarcodeStatus.CLAIMED);
	}

	@Test
	void expireReturnableBarcodesAtSystemCutoffSendsDigestPerBusiness() {
		Business otherBusiness = business("Other Store", 2L);
		ReturnableDigest firstDigest = new ReturnableDigest(business, 3L);
		ReturnableDigest secondDigest = new ReturnableDigest(otherBusiness, 2L);
		when(productBarcodeRepository.countReturnableExpiryDigestByBusiness(ProductBarcodeStatus.NOT_CLAIMED))
				.thenReturn(List.of(firstDigest, secondDigest));
		when(productBarcodeRepository.updateStatusForReturnableProducts(
				ProductBarcodeStatus.NOT_CLAIMED,
				ProductBarcodeStatus.EXPIRED))
				.thenReturn(5);

		int expired = serviceAt(LocalDateTime.of(2026, 6, 5, 17, 0))
				.expireReturnableBarcodesIfNeeded("system");

		assertThat(expired).isEqualTo(5);
		verify(appEmailService).sendReturnableBarcodesExpiredEmail(business, 3L, "system");
		verify(appEmailService).sendReturnableBarcodesExpiredEmail(otherBusiness, 2L, "system");
	}

	@Test
	void claimByIdMarksSelectedBarcodeAsClaimed() {
		ProductBarcode barcode = barcode(14L, true, ProductBarcodeStatus.NOT_CLAIMED);
		when(productBarcodeRepository.findWithProductById(14L, 1L)).thenReturn(Optional.of(barcode));
		when(productBarcodeRepository.updateStatus(14L, ProductBarcodeStatus.CLAIMED)).thenReturn(1);

		ProductBarcode result = serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.claimById(14L, "worker");

		assertThat(result.getStatus()).isEqualTo(ProductBarcodeStatus.CLAIMED);
		verify(productBarcodeRepository).updateStatus(14L, ProductBarcodeStatus.CLAIMED);
	}

	@Test
	void claimByReferenceRejectsExpiredBarcode() {
		ProductBarcode barcode = barcode(14L, true, ProductBarcodeStatus.EXPIRED);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(barcode));

		assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.claimByReference("customer@example.com", "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Returnable barcode claim has expired");
	}

	@Test
	void claimByReferenceRejectsMultipleActiveBarcodesForSameReference() {
		ProductBarcode first = barcode(14L, true, ProductBarcodeStatus.NOT_CLAIMED);
		ProductBarcode second = barcode(15L, true, ProductBarcodeStatus.NOT_CLAIMED);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(first, second));

		assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.claimByReference("customer@example.com", "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Multiple not claimed barcodes found for reference; claim by barcode id");
	}

	@Test
	void claimByReferenceRejectsNonReturnableBarcode() {
		ProductBarcode barcode = barcode(14L, false, ProductBarcodeStatus.CLAIMED);
		when(productBarcodeRepository.findByReference("customer@example.com", 1L)).thenReturn(List.of(barcode));

		assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.claimByReference("customer@example.com", "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Only returnable barcodes can be claimed");
	}

	@Test
	void findByReferenceThrowsWhenReferenceHasNoBarcodes() {
		when(productBarcodeRepository.findByReference("missing@example.com", 1L)).thenReturn(List.of());

		assertThatThrownBy(() -> serviceAt(LocalDateTime.of(2026, 6, 1, 10, 0))
				.findByReference("missing@example.com", "worker"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Barcode not found for reference: missing@example.com");
	}

	private ProductBarcodeService serviceAt(LocalDateTime dateTime) {
		Clock clock = Clock.fixed(dateTime.atZone(BUSINESS_ZONE).toInstant(), BUSINESS_ZONE);
		return new ProductBarcodeService(productBarcodeRepository, auditLogService, clock, userService, appEmailService);
	}

	private ProductBarcode barcode(Long id, boolean bottleReturnable, ProductBarcodeStatus status) {
		Product product = new Product(
				"Castle Lite",
				ProductCategory.Alcohol,
				new BigDecimal("20.50"),
				10,
				bottleReturnable);
		product.setBusiness(business);
		ProductBarcode barcode = new ProductBarcode("6001");
		barcode.setReferenceEmail("customer@example.com");
		barcode.setStatus(status);
		product.addBarcode(barcode);
		ReflectionTestUtils.setField(product, "id", 9L);
		ReflectionTestUtils.setField(barcode, "id", id);
		return barcode;
	}

	private Business business(String name, Long id) {
		TrackerUser owner = new TrackerUser(
				name.toLowerCase().replace(" ", "-"),
				name.toLowerCase().replace(" ", "-") + "@example.com",
				"encoded",
				new com.king_sparkon_tracker.backend.model.Privilege(com.king_sparkon_tracker.backend.model.PrivilegeRole.Owner));
		Business business = new Business(name, owner);
		ReflectionTestUtils.setField(business, "id", id);
		owner.setBusiness(business);
		return business;
	}

	private record ReturnableDigest(Business business, long expiredCount)
			implements com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository.ReturnableBarcodeExpiryDigest {
		@Override
		public Business getBusiness() {
			return business;
		}

		@Override
		public long getExpiredCount() {
			return expiredCount;
		}
	}
}
