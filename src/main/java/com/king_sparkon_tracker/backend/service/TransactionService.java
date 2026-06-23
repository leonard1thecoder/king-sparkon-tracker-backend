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

import com.king_sparkon_tracker.backend.dto.CreateTransactionRequest;
import com.king_sparkon_tracker.backend.dto.TransactionItemRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.service.StripeService.CreatedTransactionPaymentLink;

@Service
@Transactional
public class TransactionService {

	private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

	private final InventoryTransactionRepository transactionRepository;
	private final ProductService productService;
	private final TrackerUserService userService;
	private final AuditLogService auditLogService;
	private final ProductPricingService productPricingService;
	private final ProductBarcodeRepository productBarcodeRepository;
	private final AppEmailService appEmailService;
	private final StripeService stripeService;

	public TransactionService(
			InventoryTransactionRepository transactionRepository,
			ProductService productService,
			TrackerUserService userService,
			AuditLogService auditLogService,
			ProductPricingService productPricingService,
			ProductBarcodeRepository productBarcodeRepository,
			AppEmailService appEmailService,
			StripeService stripeService) {
		this.transactionRepository = transactionRepository;
		this.productService = productService;
		this.userService = userService;
		this.auditLogService = auditLogService;
		this.productPricingService = productPricingService;
		this.productBarcodeRepository = productBarcodeRepository;
		this.appEmailService = appEmailService;
		this.stripeService = stripeService;
	}

	public InventoryTransaction createTransaction(CreateTransactionRequest request) {
		throw new IllegalArgumentException("Transaction creation requires an authenticated business user");
	}

	public InventoryTransaction createTransaction(CreateTransactionRequest request, String actorUsername) {
		TransactionType type = requirePresent(request.type(), "Transaction type is required");
		List<TransactionItemRequest> itemRequests = requireItems(request.items());
		TransactionPaymentType paymentType = paymentTypeFor(type, request.paymentType());
		String paymentEmail = paymentEmailFor(type, paymentType, request.paymentEmail());

		Business business = userService.businessForActor(actorUsername);

		TrackerUser employee = requireUserWithRole(
				request.employeeId(),
				PrivilegeRole.Worker,
				"Employee");

		TrackerUser owner = requireUserWithRole(
				request.ownerId(),
				PrivilegeRole.Owner,
				"Owner");

		requireSameBusiness(employee, business, "Employee");
		requireSameBusiness(owner, business, "Owner");

		Map<Long, Product> lockedProducts = lockProductsForTransaction(itemRequests, business.getId());

		InventoryTransaction transaction = new InventoryTransaction(type, employee, owner, business);
		if (type == TransactionType.SELL && paymentType != TransactionPaymentType.WEBSITE_PAYMENT) {
			transaction.markOfflinePayment(paymentType, paymentEmail);
		} else if (type == TransactionType.SELL) {
			transaction.prepareWebsitePayment(paymentEmail);
		}

		for (TransactionItemRequest itemRequest : itemRequests) {
			Long productId = requirePresent(itemRequest.productId(), "Product id is required");
			Product product = lockedProducts.get(productId);
			String referenceEmail = referenceEmailForItem(product, type, itemRequest.referenceEmail(), paymentEmail);

			int quantity = requirePositive(itemRequest.quantity(), "Quantity must be greater than zero");

			BigDecimal overrideUnitPrice = itemRequest.unitPrice() == null
					? null
					: requireNonNegative(itemRequest.unitPrice(), "Unit price cannot be negative");

			BigDecimal unitPrice = productPricingService.priceForTransaction(product, type, overrideUnitPrice);

			List<String> barcodes = barcodesForItem(product, type, quantity, itemRequest.barcodes());

			productService.applyStockMovement(product, type, quantity);

			if (type == TransactionType.SELL) {
				markBarcodesAsSold(product, barcodes, referenceEmail);
			}

			transaction.addItem(new TransactionItem(product, quantity, unitPrice, barcodes));
		}

		InventoryTransaction savedTransaction = transactionRepository.save(transaction);
		if (type == TransactionType.SELL && paymentType == TransactionPaymentType.WEBSITE_PAYMENT) {
			CreatedTransactionPaymentLink paymentLink = stripeService.createTransactionPaymentLink(savedTransaction);
			savedTransaction.markWebsitePaymentPending(paymentEmail, paymentLink.stripeId(), paymentLink.paymentUrl());
			savedTransaction = transactionRepository.save(savedTransaction);
			sendTransactionWebsitePaymentEmail(savedTransaction, actorUsername);
		}

		auditLogService.record(
				"TRANSACTION_CREATED",
				"InventoryTransaction",
				String.valueOf(savedTransaction.getId()),
				actorUsername,
				"Transaction type: " + savedTransaction.getType() + ", items: " + savedTransaction.getItems().size(),
				business);

		log.info(
				"transaction_created transactionId={} businessId={} type={} employeeId={} ownerId={} itemCount={} actor={}",
				savedTransaction.getId(),
				business.getId(),
				savedTransaction.getType(),
				employee.getId(),
				owner.getId(),
				savedTransaction.getItems().size(),
				actorUsername);

		sendTransactionCreatedNotifications(savedTransaction, actorUsername);

		return savedTransaction;
	}

	@Transactional(readOnly = true)
	public List<InventoryTransaction> listTransactions() {
		log.debug("transactions_list_requested mode=all");
		return transactionRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Page<InventoryTransaction> listTransactions(Pageable pageable) {
		log.debug("transactions_list_requested page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
		return transactionRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Page<InventoryTransaction> listTransactions(Pageable pageable, String actorUsername) {
		Business business = userService.businessForActor(actorUsername);

		log.debug(
				"transactions_list_requested businessId={} page={} size={}",
				business.getId(),
				pageable.getPageNumber(),
				pageable.getPageSize());

		return transactionRepository.findByBusiness_Id(business.getId(), pageable);
	}

	@Transactional(readOnly = true)
	public InventoryTransaction getTransactionById(Long id) {
		return transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
	}

	@Transactional(readOnly = true)
	public InventoryTransaction getTransactionById(Long id, String actorUsername) {
		Business business = userService.businessForActor(actorUsername);

		return transactionRepository.findByIdAndBusiness_Id(id, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
	}

	public void handleWebsitePaymentSucceeded(Long transactionId, String paymentReference, String stripeEventId) {
		InventoryTransaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
		if (transaction.getType() != TransactionType.SELL
				|| transaction.getPaymentType() != TransactionPaymentType.WEBSITE_PAYMENT) {
			throw new IllegalArgumentException("Stripe website payment event does not belong to a website-payment sale transaction");
		}
		if (transaction.getPaymentStatus() == TransactionPaymentStatus.PAID) {
			log.info("transaction_website_payment_already_paid transactionId={} stripeEventId={}", transactionId, stripeEventId);
			return;
		}

		transaction.markWebsitePaymentPaid(paymentReference);
		transactionRepository.save(transaction);
		log.info("transaction_website_payment_paid transactionId={} stripeEventId={}", transactionId, stripeEventId);
	}

	private TrackerUser requireUserWithRole(Long userId, PrivilegeRole role, String label) {
		requirePresent(userId, label + " id is required");

		TrackerUser user = userService.getUserById(userId);

		if (user.getPrivilege().getName() != role) {
			log.warn(
					"transaction_user_role_rejected userId={} expectedRole={} actualRole={} label={}",
					userId,
					role,
					user.getPrivilege().getName(),
					label);

			throw new IllegalArgumentException(label + " must have role " + role);
		}

		return user;
	}

	private void requireSameBusiness(TrackerUser user, Business business, String label) {
		if (user.getBusiness() == null || !Objects.equals(user.getBusiness().getId(), business.getId())) {
			log.warn(
					"transaction_user_business_rejected userId={} expectedBusinessId={} actualBusinessId={} label={}",
					user.getId(),
					business.getId(),
					user.getBusiness() == null ? null : user.getBusiness().getId(),
					label);

			throw new IllegalArgumentException(label + " must belong to the authenticated business");
		}
	}

	private List<TransactionItemRequest> requireItems(List<TransactionItemRequest> items) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("Transaction must contain at least one item");
		}

		return items;
	}

	private TransactionPaymentType paymentTypeFor(TransactionType type, TransactionPaymentType paymentType) {
		if (type != TransactionType.SELL) {
			return null;
		}
		return requirePresent(paymentType, "SELL transaction payment type is required");
	}

	private String paymentEmailFor(
			TransactionType type,
			TransactionPaymentType paymentType,
			String requestedPaymentEmail) {
		if (type != TransactionType.SELL) {
			return null;
		}
		String paymentEmail = EmailAddressNormalizer.normalizeOptional(
				requestedPaymentEmail,
				"Payment email must be a valid email address");
		if (paymentType == TransactionPaymentType.WEBSITE_PAYMENT && paymentEmail == null) {
			throw new IllegalArgumentException("Website payment requires paymentEmail");
		}
		return paymentEmail;
	}

	private String referenceEmailForItem(
			Product product,
			TransactionType type,
			String requestedReferenceEmail,
			String transactionPaymentEmail) {
		if (type != TransactionType.SELL) {
			return null;
		}
		String referenceEmail = EmailAddressNormalizer.normalizeOptional(
				requestedReferenceEmail,
				"Reference email must be a valid email address");
		if (referenceEmail != null) {
			return referenceEmail;
		}
		if (product.isReturnableEnabled()) {
			if (transactionPaymentEmail == null) {
				throw new IllegalArgumentException("Returnable barcode reference email is required");
			}
			return transactionPaymentEmail;
		}
		return null;
	}

	private Map<Long, Product> lockProductsForTransaction(List<TransactionItemRequest> items, Long businessId) {
		return items.stream()
				.map(item -> requirePresent(item.productId(), "Product id is required"))
				.distinct()
				.sorted()
				.collect(Collectors.toMap(
						Function.identity(),
						productId -> productService.getProductForStockUpdate(productId, businessId),
						(left, right) -> left,
						LinkedHashMap::new));
	}

	private List<String> barcodesForItem(
			Product product,
			TransactionType type,
			int quantity,
			List<String> requestedBarcodes) {
		List<String> barcodes = normalizeBarcodes(requestedBarcodes);

		if (type != TransactionType.SELL) {
			if (!barcodes.isEmpty()) {
				throw new IllegalArgumentException("Barcodes can only be attached to SELL transactions");
			}

			return List.of();
		}

		if (barcodes.isEmpty()) {
			throw new IllegalArgumentException("SELL transaction requires scanned barcodes");
		}

		if (barcodes.size() != quantity) {
			throw new IllegalArgumentException("SELL transaction barcode count must match quantity");
		}

		requireUniqueBarcodes(barcodes);
		requireBarcodesBelongToProductAndAreAvailable(product, barcodes);

		return barcodes;
	}

	private List<String> normalizeBarcodes(List<String> barcodes) {
		if (barcodes == null || barcodes.isEmpty()) {
			return List.of();
		}

		List<String> normalized = new ArrayList<>();

		for (String barcode : barcodes) {
			if (barcode != null && !barcode.isBlank()) {
				normalized.add(barcode.trim());
			}
		}

		return normalized;
	}

	private void requireUniqueBarcodes(List<String> barcodes) {
		Set<String> uniqueBarcodes = new LinkedHashSet<>(barcodes);

		if (uniqueBarcodes.size() != barcodes.size()) {
			throw new IllegalArgumentException("SELL transaction barcodes must be unique");
		}
	}

	private void requireBarcodesBelongToProductAndAreAvailable(Product product, List<String> barcodes) {
		Map<String, ProductBarcode> existingBarcodes = new HashMap<>();

		for (ProductBarcode productBarcode : productBarcodeRepository.findByBarcodeIn(barcodes)) {
			existingBarcodes.put(productBarcode.getBarcode(), productBarcode);
		}

		for (String barcode : barcodes) {
			ProductBarcode productBarcode = existingBarcodes.get(barcode);

			if (productBarcode == null || !Objects.equals(product.getId(), productBarcode.getProduct().getId())) {
				throw new IllegalArgumentException("Every SELL barcode must be registered to the selected product");
			}

			if (productBarcode.getAvailabilityStatus() != ProductBarcodeAvailabilityStatus.AVAILABLE) {
				throw new IllegalArgumentException("Barcode is already sold or unavailable: " + barcode);
			}
		}
	}

	private void markBarcodesAsSold(Product product, List<String> barcodes, String referenceEmail) {
		if (barcodes == null || barcodes.isEmpty()) {
			return;
		}

		List<ProductBarcode> productBarcodes = productBarcodeRepository.findByBarcodeIn(barcodes);

		for (ProductBarcode productBarcode : productBarcodes) {
			if (productBarcode.getAvailabilityStatus() != ProductBarcodeAvailabilityStatus.AVAILABLE) {
				throw new IllegalArgumentException("Barcode is already sold or unavailable: " + productBarcode.getBarcode());
			}

			productBarcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.SOLD);
			if (product.isReturnableEnabled()) {
				productBarcode.setReferenceEmail(referenceEmail);
			}
		}

		productBarcodeRepository.saveAll(productBarcodes);
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

	private BigDecimal requireNonNegative(BigDecimal value, String message) {
		requirePresent(value, message);

		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException(message);
		}

		return value;
	}

	private void sendTransactionCreatedNotifications(InventoryTransaction transaction, String actorUsername) {
		try {
			appEmailService.sendTransactionCreatedOwnerEmail(transaction);
		} catch (RuntimeException exception) {
			log.warn(
					"transaction_created_owner_email_failed_non_blocking recipient={} transactionId={} businessId={} actor={} reason={}",
					AppEmailService.maskEmail(transaction.getOwner().getEmailAddress()),
					transaction.getId(),
					transaction.getBusiness() == null ? null : transaction.getBusiness().getId(),
					actorUsername,
					exception.getMessage());
		}

		if (Objects.equals(transaction.getEmployee().getUsername(), actorUsername)) {
			return;
		}

		try {
			appEmailService.sendTransactionCreatedWorkerEmail(transaction);
		} catch (RuntimeException exception) {
			log.warn(
					"transaction_created_worker_email_failed_non_blocking recipient={} transactionId={} businessId={} actor={} reason={}",
					AppEmailService.maskEmail(transaction.getEmployee().getEmailAddress()),
					transaction.getId(),
					transaction.getBusiness() == null ? null : transaction.getBusiness().getId(),
					actorUsername,
					exception.getMessage());
		}
	}

	private void sendTransactionWebsitePaymentEmail(InventoryTransaction transaction, String actorUsername) {
		try {
			appEmailService.sendTransactionWebsitePaymentEmail(transaction);
		} catch (RuntimeException exception) {
			log.warn(
					"transaction_website_payment_email_failed_non_blocking recipient={} transactionId={} businessId={} actor={} reason={}",
					AppEmailService.maskEmail(transaction.getPaymentEmail()),
					transaction.getId(),
					transaction.getBusiness() == null ? null : transaction.getBusiness().getId(),
					actorUsername,
					exception.getMessage());
		}
	}
}
