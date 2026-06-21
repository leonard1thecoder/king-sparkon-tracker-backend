package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;

@ExtendWith(MockitoExtension.class)
class ProductBarcodeClaimServiceTest {

	@Mock
	private ProductBarcodeRepository productBarcodeRepository;

	@Mock
	private TrackerUserService userService;

	@Mock
	private BusinessAccessService businessAccessService;

	private ProductBarcodeClaimService service;
	private Business business;

	@BeforeEach
	void setUp() {
		service = new ProductBarcodeClaimService(
				productBarcodeRepository,
				userService,
				businessAccessService,
				Clock.fixed(Instant.parse("2026-06-05T15:00:00Z"), ZoneId.of("Africa/Johannesburg")));
		business = business();
	}

	@Test
	void claimBarcodeMarksActiveReturnableBarcodeClaimed() {
		ProductBarcode barcode = barcode(ProductBarcodeStatus.NOT_CLAIMED);
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productBarcodeRepository.findWithProductById(5L, business.getId())).thenReturn(Optional.of(barcode));
		when(productBarcodeRepository.save(barcode)).thenReturn(barcode);

		ProductBarcode result = service.claimBarcode(5L, "worker");

		assertThat(result.getStatus()).isEqualTo(ProductBarcodeStatus.CLAIMED);
		verify(businessAccessService).requireFeature("worker", BusinessFeature.SCAN_BARCODES);
		verify(productBarcodeRepository).save(barcode);
	}

	@Test
	void claimBarcodeRejectsNullId() {
		assertThatThrownBy(() -> service.claimBarcode(null, "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Barcode id is required");

		verify(businessAccessService).requireFeature("worker", BusinessFeature.SCAN_BARCODES);
	}

	@Test
	void claimBarcodeRejectsMissingBarcode() {
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productBarcodeRepository.findWithProductById(5L, business.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.claimBarcode(5L, "worker"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Product barcode not found: 5");
	}

	@Test
	void claimBarcodeRejectsNonReturnableBarcode() {
		assertRejectedStatus(
				ProductBarcodeStatus.NOT_CLAIMABLE,
				"This barcode belongs to a non-returnable product and cannot be claimed");
	}

	@Test
	void claimBarcodeRejectsExpiredBarcode() {
		assertRejectedStatus(
				ProductBarcodeStatus.EXPIRED,
				"This barcode claim has expired");
	}

	@Test
	void claimBarcodeRejectsAlreadyClaimedBarcode() {
		assertRejectedStatus(
				ProductBarcodeStatus.CLAIMED,
				"This barcode was already claimed");
	}

	@Test
	void expireUnclaimedBarcodesMovesAllNotClaimedRowsToExpired() {
		when(productBarcodeRepository.updateStatusForAllMatchingStatus(
				ProductBarcodeStatus.NOT_CLAIMED,
				ProductBarcodeStatus.EXPIRED)).thenReturn(7);

		assertThat(service.expireUnclaimedBarcodes()).isEqualTo(7);
	}

	private void assertRejectedStatus(ProductBarcodeStatus status, String message) {
		when(userService.businessForActor("worker")).thenReturn(business);
		when(productBarcodeRepository.findWithProductById(5L, business.getId()))
				.thenReturn(Optional.of(barcode(status)));

		assertThatThrownBy(() -> service.claimBarcode(5L, "worker"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	private ProductBarcode barcode(ProductBarcodeStatus status) {
		ProductBarcode barcode = new ProductBarcode("6001");
		ReflectionTestUtils.setField(barcode, "id", 5L);
		barcode.setStatus(status);
		return barcode;
	}

	private Business business() {
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Business business = new Business("Owner Store", owner);
		ReflectionTestUtils.setField(business, "id", 1L);
		owner.setBusiness(business);
		return business;
	}
}
