package com.king_sparkon_tracker.backend.inventory.reservation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseItemRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@Service
@Transactional
public class StockReservationService {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

	private final StockReservationRepository reservationRepository;
	private final ProductRepository productRepository;
	private final ProductBarcodeRepository barcodeRepository;
	private final ObjectMapper objectMapper;

	public StockReservationService(
			StockReservationRepository reservationRepository,
			ProductRepository productRepository,
			ProductBarcodeRepository barcodeRepository,
			ObjectMapper objectMapper) {
		this.reservationRepository = reservationRepository;
		this.productRepository = productRepository;
		this.barcodeRepository = barcodeRepository;
		this.objectMapper = objectMapper;
	}

	public List<StockReservation> reserveTransaction(InventoryTransaction transaction, String idempotencyKey) {
		if (transaction == null || transaction.getId() == null || !StringUtils.hasText(transaction.getPaymentReference())) {
			throw new IllegalArgumentException("Persisted website-payment transaction and payment reference are required");
		}
		List<StockReservation> reservations = new ArrayList<>();
		for (TransactionItem item : transaction.getItems()) {
			reservations.add(reserve(
					transaction.getPaymentReference(),
					idempotencyKey,
					transaction,
					item.getProduct().getId(),
					transaction.getBusiness().getId(),
					item.getQuantity(),
					item.getBarcodes(),
					transaction.getPaymentContact(),
					DEFAULT_TTL));
		}
		return reservations;
	}

	public List<StockReservation> reserveCart(
			String paymentReference,
			String idempotencyKey,
			List<TuckShopPurchaseItemRequest> items) {
		List<StockReservation> reservations = new ArrayList<>();
		for (TuckShopPurchaseItemRequest item : items) {
			Product product = productRepository.findLockedById(item.productId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.productId()));
			if (product.getBusiness() == null || product.getBusiness().getId() == null) {
				throw new IllegalArgumentException("Product is not linked to a business");
			}
			reservations.add(reserve(
					paymentReference,
					idempotencyKey,
					null,
					product.getId(),
					product.getBusiness().getId(),
					item.quantity(),
					item.barcodes(),
					null,
					DEFAULT_TTL));
		}
		return reservations;
	}

	public void bindAndConsume(String paymentReference, InventoryTransaction transaction) {
		List<StockReservation> reservations = reservationRepository.findLockedByPaymentReference(paymentReference);
		if (reservations.isEmpty()) {
			throw new IllegalStateException("No active stock reservation exists for payment " + paymentReference);
		}
		for (StockReservation reservation : reservations) {
			if (!reservation.getBusiness().getId().equals(transaction.getBusiness().getId())) {
				continue;
			}
			reservation.bind(transaction);
			consume(reservation);
		}
	}

	public void consumeTransaction(Long transactionId) {
		List<StockReservation> reservations = reservationRepository.findLockedByTransactionId(transactionId);
		if (reservations.isEmpty()) {
			throw new IllegalStateException("Website payment has no stock reservations for transaction " + transactionId);
		}
		reservations.forEach(this::consume);
	}

	public void releaseByPaymentReference(String paymentReference) {
		if (!StringUtils.hasText(paymentReference)) return;
		reservationRepository.findLockedByPaymentReference(paymentReference).forEach(reservation -> release(reservation, false));
	}

	public void releaseByTransaction(Long transactionId) {
		reservationRepository.findLockedByTransactionId(transactionId).forEach(reservation -> release(reservation, false));
	}

	@Scheduled(fixedDelayString = "${app.stock-reservations.expiry-delay-ms:60000}")
	public void expireReservations() {
		reservationRepository.findExpiredLocked(List.of(StockReservationStatus.ACTIVE), Instant.now())
				.forEach(reservation -> release(reservation, true));
	}

	private StockReservation reserve(
			String paymentReference,
			String idempotencyKey,
			InventoryTransaction transaction,
			Long productId,
			Long businessId,
			Integer requestedQuantity,
			List<String> requestedUnitCodes,
			String referenceEmail,
			Duration ttl) {
		StockReservation existing = reservationRepository
				.findByPaymentReferenceAndProduct_Id(paymentReference, productId)
				.orElse(null);
		if (existing != null) {
			if (existing.getQuantity() != requestedQuantity) {
				throw new IllegalArgumentException("Payment reference already reserves a different product quantity");
			}
			return existing;
		}
		Product product = productRepository.findLockedByIdAndBusiness_Id(productId, businessId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found in business: " + productId));
		int quantity = requestedQuantity == null ? 0 : requestedQuantity;
		if (quantity <= 0) throw new IllegalArgumentException("Reservation quantity must be greater than zero");
		if (product.getStockQuantity() < quantity) {
			throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
		}
		List<String> unitCodes = normalized(requestedUnitCodes);
		if (!unitCodes.isEmpty() && unitCodes.size() != quantity) {
			throw new IllegalArgumentException("Reserved stock unit count must match quantity");
		}
		for (String unitCode : unitCodes) {
			ProductBarcode barcode = barcodeRepository.findByUnitCode(unitCode, businessId)
					.orElseThrow(() -> new ResourceNotFoundException("Stock unit not found: " + unitCode));
			if (!barcode.getProduct().getId().equals(productId)
					|| barcode.getAvailabilityStatus() != ProductBarcodeAvailabilityStatus.AVAILABLE) {
				throw new IllegalArgumentException("Stock unit is unavailable: " + unitCode);
			}
			barcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.RESERVED);
			barcode.setReferenceEmail(referenceEmail);
			barcodeRepository.save(barcode);
		}
		product.setStockQuantity(product.getStockQuantity() - quantity);
		productRepository.save(product);
		return reservationRepository.save(new StockReservation(
				product.getBusiness(),
				product,
				transaction,
				paymentReference,
				idempotencyKey,
				quantity,
				json(unitCodes),
				referenceEmail,
				Instant.now().plus(ttl)));
	}

	private void consume(StockReservation reservation) {
		if (reservation.getStatus() == StockReservationStatus.CONSUMED) return;
		for (String unitCode : unitCodes(reservation)) {
			ProductBarcode barcode = barcodeRepository.findByUnitCode(unitCode, reservation.getBusiness().getId())
					.orElseThrow(() -> new ResourceNotFoundException("Reserved stock unit not found: " + unitCode));
			if (barcode.getAvailabilityStatus() != ProductBarcodeAvailabilityStatus.RESERVED) {
				throw new IllegalStateException("Reserved stock unit is no longer reserved: " + unitCode);
			}
			barcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.SOLD);
			barcode.setReferenceEmail(reservation.getReferenceEmail());
			barcodeRepository.save(barcode);
		}
		reservation.consume();
		reservationRepository.save(reservation);
	}

	private void release(StockReservation reservation, boolean expired) {
		if (reservation.getStatus() != StockReservationStatus.ACTIVE) return;
		Product product = productRepository.findLockedByIdAndBusiness_Id(
				reservation.getProduct().getId(), reservation.getBusiness().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Reserved product no longer exists"));
		product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
		productRepository.save(product);
		for (String unitCode : unitCodes(reservation)) {
			barcodeRepository.findByUnitCode(unitCode, reservation.getBusiness().getId()).ifPresent(barcode -> {
				if (barcode.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.RESERVED) {
					barcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.AVAILABLE);
					barcode.setReferenceEmail(null);
					barcodeRepository.save(barcode);
				}
			});
		}
		reservation.release(expired);
		reservationRepository.save(reservation);
	}

	private List<String> normalized(List<String> values) {
		if (values == null) return List.of();
		return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
	}

	private String json(List<String> values) {
		try { return objectMapper.writeValueAsString(values); }
		catch (JsonProcessingException exception) { throw new IllegalArgumentException("Could not serialize reservation stock units", exception); }
	}

	private List<String> unitCodes(StockReservation reservation) {
		if (!StringUtils.hasText(reservation.getUnitCodes())) return List.of();
		try { return objectMapper.readValue(reservation.getUnitCodes(), new TypeReference<>() {}); }
		catch (JsonProcessingException exception) { throw new IllegalStateException("Could not read reservation stock units", exception); }
	}
}
