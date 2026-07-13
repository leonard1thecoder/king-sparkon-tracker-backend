package com.king_sparkon_tracker.backend.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TuckShopFulfilmentStatus;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;

@Service
@Transactional
public class TuckShopCollectionService {

	private static final String COLLECTION_PREFIX = "KST-COLLECT:";

	private final InventoryTransactionRepository transactionRepository;
	private final ProductBarcodeRepository productBarcodeRepository;
	private final TrackerUserService userService;
	private final AuditLogService auditLogService;

	public TuckShopCollectionService(
			InventoryTransactionRepository transactionRepository,
			ProductBarcodeRepository productBarcodeRepository,
			TrackerUserService userService,
			AuditLogService auditLogService) {
		this.transactionRepository = transactionRepository;
		this.productBarcodeRepository = productBarcodeRepository;
		this.userService = userService;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<TuckShopPurchaseResponse> listWorkerOnlinePurchases(String actorUsername) {
		TrackerUser worker = requireWorker(actorUsername);
		Business business = requireBusiness(worker);
		return transactionRepository.findByBusiness_IdAndPaymentStatusAndFulfilmentStatusInOrderByDateAsc(
				business.getId(),
				TransactionPaymentStatus.PAID,
				List.of(
						TuckShopFulfilmentStatus.AWAITING_BARCODE_ASSIGNMENT,
						TuckShopFulfilmentStatus.READY_FOR_COLLECTION))
				.stream()
				.map(transaction -> TuckShopPurchaseResponse.from(transaction, null))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TuckShopPurchaseResponse> listCurrentUserPurchases(String actorUsername) {
		TrackerUser customer = userService.getUserByUsername(required(actorUsername, "Authenticated username is required"));
		return transactionRepository.findByCustomer_IdOrderByDateDesc(customer.getId())
				.stream()
				.map(transaction -> TuckShopPurchaseResponse.from(transaction, null))
				.toList();
	}

	public TuckShopPurchaseResponse assignBarcode(
			Long transactionId,
			Long productId,
			String scannedValue,
			String actorUsername) {
		TrackerUser worker = requireWorker(actorUsername);
		Business business = requireBusiness(worker);
		InventoryTransaction transaction = transactionRepository.findByIdAndBusiness_Id(transactionId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Online purchase not found: " + transactionId));

		requirePaidCollectionOrder(transaction);
		if (transaction.getFulfilmentStatus() == TuckShopFulfilmentStatus.READY_FOR_COLLECTION) {
			throw new IllegalStateException("All purchased units already have assigned barcodes");
		}

		TransactionItem item = transaction.getItems().stream()
				.filter(candidate -> Objects.equals(candidate.getProduct().getId(), productId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Product is not part of this purchase: " + productId));

		if (item.getBarcodes().size() >= item.getQuantity()) {
			throw new IllegalStateException("This product line already has every required barcode");
		}

		ProductBarcode barcode = resolveAvailableBarcode(item.getProduct(), business.getId(), scannedValue);
		item.addBarcode(barcode.getUnitCode());
		barcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.SOLD);
		if (item.getProduct().isReturnableEnabled()) {
			barcode.setReferenceEmail(transaction.getPaymentEmail());
		}
		productBarcodeRepository.save(barcode);

		if (transaction.getOutstandingBarcodeCount() == 0) {
			transaction.markReadyForCollection(worker.getId());
		}
		InventoryTransaction saved = transactionRepository.save(transaction);

		auditLogService.record(
				"ONLINE_PURCHASE_BARCODE_ASSIGNED",
				"InventoryTransaction",
				String.valueOf(saved.getId()),
				actorUsername,
				"Assigned stock unit " + barcode.getUnitCode() + " to product " + item.getProduct().getId()
						+ "; remaining barcodes: " + saved.getOutstandingBarcodeCount(),
				business);

		return TuckShopPurchaseResponse.from(saved, null);
	}

	public TuckShopPurchaseResponse verifyCollection(String qrValue, String actorUsername) {
		TrackerUser customer = userService.getUserByUsername(required(actorUsername, "Authenticated username is required"));
		String collectionToken = collectionToken(qrValue);
		InventoryTransaction transaction = transactionRepository.findByCollectionToken(collectionToken)
				.orElseThrow(() -> new ResourceNotFoundException("Collection cart was not found"));

		if (transaction.getCustomer() == null || !Objects.equals(transaction.getCustomer().getId(), customer.getId())) {
			throw new IllegalArgumentException("This collection QR does not belong to the authenticated user");
		}
		if (transaction.getFulfilmentStatus() == TuckShopFulfilmentStatus.COLLECTED) {
			return TuckShopPurchaseResponse.from(transaction, null);
		}
		if (transaction.getPaymentStatus() != TransactionPaymentStatus.PAID) {
			throw new IllegalStateException("The product order must be paid before collection");
		}
		if (transaction.getFulfilmentStatus() != TuckShopFulfilmentStatus.READY_FOR_COLLECTION
				|| transaction.getOutstandingBarcodeCount() != 0) {
			throw new IllegalStateException("The worker has not finished preparing this order for collection");
		}

		transaction.markCollected();
		InventoryTransaction saved = transactionRepository.save(transaction);
		auditLogService.record(
				"ONLINE_PURCHASE_COLLECTED",
				"InventoryTransaction",
				String.valueOf(saved.getId()),
				actorUsername,
				"Authenticated customer verified the worker collection QR and collected the product order",
				saved.getBusiness());
		return TuckShopPurchaseResponse.from(saved, null);
	}

	private ProductBarcode resolveAvailableBarcode(Product product, Long businessId, String scannedValue) {
		String value = required(scannedValue, "Scan or enter a product barcode");

		ProductBarcode exactUnit = productBarcodeRepository.findByUnitCode(value, businessId).orElse(null);
		if (isAvailableForProduct(exactUnit, product)) {
			return exactUnit;
		}

		ProductBarcode reusableMatch = productBarcodeRepository.findByBarcode(value, businessId).stream()
				.filter(candidate -> isAvailableForProduct(candidate, product))
				.findFirst()
				.orElse(null);
		if (reusableMatch != null) {
			return reusableMatch;
		}

		if (StringUtils.hasText(product.getProductBarcode()) && product.getProductBarcode().equals(value)) {
			return productBarcodeRepository.findByProduct_IdOrderByIdAsc(product.getId()).stream()
					.filter(candidate -> candidate.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.AVAILABLE)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("No available stock-unit barcode remains for " + product.getName()));
		}

		throw new IllegalArgumentException("The scanned barcode is not an available unit for " + product.getName());
	}

	private boolean isAvailableForProduct(ProductBarcode barcode, Product product) {
		return barcode != null
				&& barcode.getProduct() != null
				&& Objects.equals(barcode.getProduct().getId(), product.getId())
				&& barcode.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.AVAILABLE;
	}

	private void requirePaidCollectionOrder(InventoryTransaction transaction) {
		if (transaction.getPaymentStatus() != TransactionPaymentStatus.PAID) {
			throw new IllegalStateException("Only paid online purchases can receive collection barcodes");
		}
		if (transaction.getFulfilmentStatus() != TuckShopFulfilmentStatus.AWAITING_BARCODE_ASSIGNMENT
				&& transaction.getFulfilmentStatus() != TuckShopFulfilmentStatus.READY_FOR_COLLECTION) {
			throw new IllegalStateException("This transaction is not an online collection order");
		}
	}

	private TrackerUser requireWorker(String actorUsername) {
		TrackerUser worker = userService.getUserByUsername(required(actorUsername, "Authenticated username is required"));
		if (worker.getPrivilege() == null || worker.getPrivilege().getName() != PrivilegeRole.Worker) {
			throw new IllegalArgumentException("Only workers can prepare paid online purchases");
		}
		return worker;
	}

	private Business requireBusiness(TrackerUser worker) {
		if (worker.getBusiness() == null) {
			throw new IllegalArgumentException("Worker must belong to a business");
		}
		return worker.getBusiness();
	}

	private String collectionToken(String qrValue) {
		String value = required(qrValue, "Collection QR value is required");
		return value.startsWith(COLLECTION_PREFIX) ? value.substring(COLLECTION_PREFIX.length()) : value;
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}
}
