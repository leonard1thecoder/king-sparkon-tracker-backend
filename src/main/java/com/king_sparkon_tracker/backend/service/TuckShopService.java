package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.CreateTuckShopPurchaseRequest;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseItemRequest;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;
import com.king_sparkon_tracker.backend.service.StripeService.CreatedTransactionPaymentLink;

@Service
@Transactional
public class TuckShopService {

	private static final Logger log = LoggerFactory.getLogger(TuckShopService.class);

	private final ProductService productService;
	private final ProductRepository productRepository;
	private final ProductBarcodeRepository productBarcodeRepository;
	private final ProductPricingService productPricingService;
	private final InventoryTransactionRepository transactionRepository;
	private final TrackerUserService userService;
	private final StripeService stripeService;
	private final TipService tipService;
	private final AuditLogService auditLogService;

	public TuckShopService(
			ProductService productService,
			ProductRepository productRepository,
			ProductBarcodeRepository productBarcodeRepository,
			ProductPricingService productPricingService,
			InventoryTransactionRepository transactionRepository,
			TrackerUserService userService,
			StripeService stripeService,
			TipService tipService,
			AuditLogService auditLogService) {
		this.productService = productService;
		this.productRepository = productRepository;
		this.productBarcodeRepository = productBarcodeRepository;
		this.productPricingService = productPricingService;
		this.transactionRepository = transactionRepository;
		this.userService = userService;
		this.stripeService = stripeService;
		this.tipService = tipService;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public Page<Product> listAvailableProducts(Pageable pageable, Long businessId, ProductCategory category, String search) {
		return productService.listTuckShopProducts(pageable, businessId, category, normalizeOptional(search));
	}

	@Transactional(readOnly = true)
	public Product getAvailableProduct(Long productId) {
		Product product = productRepository.findWithBarcodesById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
		requireVisibleInTuckShop(product);
		return product;
	}

	public TuckShopPurchaseResponse createSelfServicePurchase(CreateTuckShopPurchaseRequest request, String actorUsername) {
		TrackerUser customer = userService.getUserByUsername(actorUsername);
		InventoryTransaction transaction = createPurchaseTransaction(request, customer, false, actorUsername);
		TipResponse tip = createSeparateWorkerTipIfRequested(request, transaction);
		return TuckShopPurchaseResponse.from(transaction, tip);
	}

	public TuckShopPurchaseResponse createWorkerBarcodePurchase(CreateTuckShopPurchaseRequest request, String actorUsername) {
		TrackerUser worker = userService.getUserByUsername(actorUsername);
		requireRole(worker, PrivilegeRole.Worker, "Only workers can create barcode tuck shop purchases");
		InventoryTransaction transaction = createPurchaseTransaction(request, worker, true, actorUsername);
		TipResponse tip = createSeparateWorkerTipIfRequested(request, transaction);
		return TuckShopPurchaseResponse.from(transaction, tip);
	}

	private InventoryTransaction createPurchaseTransaction(
			CreateTuckShopPurchaseRequest request,
			TrackerUser actor,
			boolean requireScannedUnitCodes,
			String actorUsername) {
		List<TuckShopPurchaseItemRequest> itemRequests = requireItems(request.items());
		Map<Long, Product> lockedProducts = lockProducts(itemRequests);
		Business business = singleBusiness(lockedProducts.values());
		TrackerUser owner = business.getOwner();
		TrackerUser worker = workerForPurchase(request.workerId(), actor, business, requireScannedUnitCodes);

		String paymentEmail = paymentEmail(request.paymentEmail(), actor);
		String paymentContact = normalizeOptional(request.paymentContact());
		if (paymentContact == null) {
			paymentContact = paymentEmail;
		}

		InventoryTransaction transaction = new InventoryTransaction(TransactionType.SELL, worker, owner, business);
		transaction.prepareWebsitePayment(paymentEmail, paymentContact);

		for (TuckShopPurchaseItemRequest itemRequest : itemRequests) {
			Long productId = requirePresent(itemRequest.productId(), "Product id is required");
			Product product = lockedProducts.get(productId);
			int quantity = requirePositive(itemRequest.quantity(), "Quantity must be greater than zero");
			List<String> unitCodes = unitCodesForPurchase(product, quantity, itemRequest.barcodes(), requireScannedUnitCodes, paymentEmail);
			BigDecimal unitPrice = productPricingService.priceForSale(product);

			productService.applyStockMovement(product, TransactionType.SELL, quantity);
			markUnitCodesAsSold(product, unitCodes, paymentEmail);
			transaction.addItem(new TransactionItem(product, quantity, unitPrice, unitCodes));
		}

		InventoryTransaction savedTransaction = transactionRepository.save(transaction);
		CreatedTransactionPaymentLink paymentLink = stripeService.createTransactionPaymentLink(savedTransaction);
		savedTransaction.markWebsitePaymentPending(paymentEmail, paymentContact, paymentLink.stripeId(), paymentLink.paymentUrl());
		savedTransaction = transactionRepository.save(savedTransaction);

		auditLogService.record(
				"TUCK_SHOP_PURCHASE_CREATED",
				"InventoryTransaction",
				String.valueOf(savedTransaction.getId()),
				actorUsername,
				"Tuck shop purchase created with product total: " + savedTransaction.getTotalAmount(),
				business);

		log.info(
				"tuck_shop_purchase_created transactionId={} businessId={} actor={} workerId={} itemCount={} total={}",
				savedTransaction.getId(),
				business.getId(),
				actorUsername,
				worker.getId(),
				savedTransaction.getItems().size(),
				savedTransaction.getTotalAmount());

		return savedTransaction;
	}

	private TipResponse createSeparateWorkerTipIfRequested(CreateTuckShopPurchaseRequest request, InventoryTransaction transaction) {
		BigDecimal tipAmount = request.tipAmount() == null ? BigDecimal.ZERO : request.tipAmount();
		if (tipAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return null;
		}
		if (request.workerId() == null) {
			throw new IllegalArgumentException("workerId is required when adding a tuck shop worker tip");
		}
		return tipService.createTip(new TipRequest(
				request.workerId(),
				tipAmount,
				request.tipCallbackUrl(),
				transaction.getPaymentContact()));
	}

	private Map<Long, Product> lockProducts(List<TuckShopPurchaseItemRequest> items) {
		return items.stream()
				.map(item -> requirePresent(item.productId(), "Product id is required"))
				.distinct()
				.sorted()
				.collect(Collectors.toMap(
						Function.identity(),
						productService::getProductForStockUpdate,
						(left, right) -> left,
						LinkedHashMap::new));
	}

	private Business singleBusiness(Iterable<Product> products) {
		Business business = null;
		for (Product product : products) {
			requireVisibleInTuckShop(product);
			Business productBusiness = product.getBusiness();
			if (productBusiness == null || productBusiness.getOwner() == null) {
				throw new IllegalArgumentException("Tuck shop product must belong to a business with an owner");
			}
			if (business == null) {
				business = productBusiness;
			} else if (!Objects.equals(business.getId(), productBusiness.getId())) {
				throw new IllegalArgumentException("One tuck shop checkout can only contain products from one business");
			}
		}
		return requirePresent(business, "Tuck shop purchase requires at least one business product");
	}

	private TrackerUser workerForPurchase(Long requestedWorkerId, TrackerUser actor, Business business, boolean requireWorkerActor) {
		TrackerUser worker;
		if (requireWorkerActor) {
			worker = actor;
		} else if (requestedWorkerId != null) {
			worker = userService.getUserById(requestedWorkerId);
			requireRole(worker, PrivilegeRole.Worker, "Selected tuck shop worker must be a worker account");
		} else {
			worker = business.getOwner();
		}

		if (worker.getBusiness() == null || !Objects.equals(worker.getBusiness().getId(), business.getId())) {
			throw new IllegalArgumentException("Tuck shop worker must belong to the product business");
		}
		return worker;
	}

	private List<String> unitCodesForPurchase(
			Product product,
			int quantity,
			List<String> requestedUnitCodes,
			boolean requireScannedUnitCodes,
			String referenceEmail) {
		List<String> normalized = normalizeBarcodes(requestedUnitCodes);
		if (requireScannedUnitCodes && normalized.isEmpty()) {
			throw new IllegalArgumentException("Worker tuck shop purchase requires scanned stock unit codes");
		}
		if (!normalized.isEmpty()) {
			if (normalized.size() != quantity) {
				throw new IllegalArgumentException("Stock unit code count must match quantity");
			}
			requireUniqueBarcodes(normalized);
			requireUnitCodesBelongToProductAndAreAvailable(product, normalized);
			return normalized;
		}

		List<String> availableUnitCodes = productBarcodeRepository.findByProduct_IdOrderByIdAsc(product.getId()).stream()
				.filter(barcode -> barcode.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.AVAILABLE)
				.map(ProductBarcode::getUnitCode)
				.limit(quantity)
				.toList();
		if (availableUnitCodes.size() != quantity) {
			throw new IllegalArgumentException("Not enough available stock units for product: " + product.getName());
		}
		if (product.isReturnableEnabled() && !StringUtils.hasText(referenceEmail)) {
			throw new IllegalArgumentException("Returnable tuck shop products require a customer reference email");
		}
		return availableUnitCodes;
	}

	private void requireVisibleInTuckShop(Product product) {
		if (product.getStatus() != ProductStatus.CREATED) {
			throw new IllegalArgumentException("Product is not available in King Sparkon Tuck Shop: " + product.getName());
		}
		if (product.getStockQuantity() <= 0) {
			throw new IllegalArgumentException("Product is out of stock: " + product.getName());
		}
	}

	private void requireUnitCodesBelongToProductAndAreAvailable(Product product, List<String> unitCodes) {
		Map<String, ProductBarcode> existingUnitCodes = new HashMap<>();
		for (ProductBarcode productBarcode : productBarcodeRepository.findByUnitCodeIn(unitCodes)) {
			existingUnitCodes.put(productBarcode.getUnitCode(), productBarcode);
		}
		for (String unitCode : unitCodes) {
			ProductBarcode productBarcode = existingUnitCodes.get(unitCode);
			if (productBarcode == null || !Objects.equals(product.getId(), productBarcode.getProduct().getId())) {
				throw new IllegalArgumentException("Every tuck shop stock unit code must be registered to the selected product");
			}
			if (productBarcode.getAvailabilityStatus() != ProductBarcodeAvailabilityStatus.AVAILABLE) {
				throw new IllegalArgumentException("Stock unit code is already sold or unavailable: " + unitCode);
			}
		}
	}

	private void markUnitCodesAsSold(Product product, List<String> unitCodes, String referenceEmail) {
		List<ProductBarcode> productBarcodes = productBarcodeRepository.findByUnitCodeIn(unitCodes);
		if (productBarcodes.size() != unitCodes.size()) {
			throw new IllegalArgumentException("Every tuck shop stock unit code must exist before it can be sold");
		}
		for (ProductBarcode productBarcode : productBarcodes) {
			if (!Objects.equals(product.getId(), productBarcode.getProduct().getId())) {
				throw new IllegalArgumentException("Every tuck shop stock unit code must belong to the selected product");
			}
			if (productBarcode.getAvailabilityStatus() != ProductBarcodeAvailabilityStatus.AVAILABLE) {
				throw new IllegalArgumentException("Stock unit code is already sold or unavailable: " + productBarcode.getUnitCode());
			}
			productBarcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.SOLD);
			if (product.isReturnableEnabled()) {
				productBarcode.setReferenceEmail(referenceEmail);
			}
		}
		productBarcodeRepository.saveAll(productBarcodes);
	}

	private List<TuckShopPurchaseItemRequest> requireItems(List<TuckShopPurchaseItemRequest> items) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("Tuck shop purchase must contain at least one item");
		}
		return items;
	}

	private List<String> normalizeBarcodes(List<String> barcodes) {
		if (barcodes == null || barcodes.isEmpty()) {
			return List.of();
		}
		List<String> normalized = new ArrayList<>();
		for (String barcode : barcodes) {
			if (StringUtils.hasText(barcode)) {
				normalized.add(barcode.trim());
			}
		}
		return normalized;
	}

	private void requireUniqueBarcodes(List<String> barcodes) {
		Set<String> uniqueBarcodes = new LinkedHashSet<>(barcodes);
		if (uniqueBarcodes.size() != barcodes.size()) {
			throw new IllegalArgumentException("Tuck shop stock unit codes must be unique");
		}
	}

	private void requireRole(TrackerUser user, PrivilegeRole role, String message) {
		if (user.getPrivilege() == null || user.getPrivilege().getName() != role) {
			throw new IllegalArgumentException(message);
		}
	}

	private String paymentEmail(String requestedEmail, TrackerUser actor) {
		String normalizedEmail = EmailAddressNormalizer.normalizeOptional(
				requestedEmail,
				"Tuck shop payment email must be a valid email address");
		return normalizedEmail == null ? actor.getEmailAddress() : normalizedEmail;
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private <T> T requirePresent(T value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

	private int requirePositive(Integer value, String message) {
		requirePresent(value, message);
		if (value <= 0) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}
}
