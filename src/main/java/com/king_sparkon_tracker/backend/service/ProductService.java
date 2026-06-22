package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.AddProductBarcodeRequest;
import com.king_sparkon_tracker.backend.dto.ProductRequest;
import com.king_sparkon_tracker.backend.dto.UpdateProductQuantityRequest;
import com.king_sparkon_tracker.backend.exception.DuplicateBarcodeException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

	private static final Logger log = LoggerFactory.getLogger(ProductService.class);

	private final ProductRepository productRepository;
	private final ProductBarcodeRepository productBarcodeRepository;
	private final AuditLogService auditLogService;
	private final TrackerUserService userService;
	private final BusinessAccessService businessAccessService;
	private final AppEmailService appEmailService;

	public ProductService(
			ProductRepository productRepository,
			ProductBarcodeRepository productBarcodeRepository,
			AuditLogService auditLogService,
			TrackerUserService userService,
			BusinessAccessService businessAccessService,
			AppEmailService appEmailService) {
		this.productRepository = productRepository;
		this.productBarcodeRepository = productBarcodeRepository;
		this.auditLogService = auditLogService;
		this.userService = userService;
		this.businessAccessService = businessAccessService;
		this.appEmailService = appEmailService;
	}

	public Product createProduct(ProductRequest request) {
		throw new IllegalArgumentException("Product creation requires a business owner");
	}

	public Product createProduct(ProductRequest request, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.CREATE_PRODUCTS);

		String name = normalizeRequired(request.name(), "Product name is required");
		ProductCategory category = requirePresent(request.category(), "Product category is required");
		BigDecimal price = requireNonNegative(request.price(), "Product price cannot be negative");
		int stockQuantity = requireNonNegative(request.stockQuantity(), "Stock quantity cannot be negative");

		boolean returnableEnabled = requirePresent(request.returnableEnabled(), "Returnable enabled flag is required");
		BigDecimal returnablePrice = moneyWhenEnabled(
				returnableEnabled,
				request.returnablePrice(),
				"Returnable price is required when returnable is enabled",
				"Returnable price cannot be negative"
		);

		boolean nightShiftEnabled = requirePresent(request.nightShiftEnabled(), "Nightshift enabled flag is required");
		BigDecimal nightShiftPrice = moneyWhenEnabled(
				nightShiftEnabled,
				request.nightShiftPrice(),
				"Nightshift price is required when nightshift is enabled",
				"Nightshift price cannot be negative"
		);

		LocalTime nightShiftStartTime = null;
		LocalTime nightShiftEndTime = null;

		if (nightShiftEnabled) {
			nightShiftStartTime = requirePresent(
					request.nightShiftStartTime(),
					"Nightshift start time is required when nightshift is enabled"
			);

			nightShiftEndTime = requirePresent(
					request.nightShiftEndTime(),
					"Nightshift end time is required when nightshift is enabled"
			);

			if (nightShiftStartTime.equals(nightShiftEndTime)) {
				throw new IllegalArgumentException("Nightshift start time and end time cannot be the same");
			}
		}

		Business business = userService.businessForActor(actorUsername);

		Product product = productRepository.save(new Product(
				name,
				category,
				price,
				stockQuantity,
				returnableEnabled,
				returnablePrice,
				nightShiftEnabled,
				nightShiftPrice,
				nightShiftStartTime,
				nightShiftEndTime,
				business
		));

		auditLogService.record(
				"PRODUCT_CREATED",
				"Product",
				String.valueOf(product.getId()),
				actorUsername,
				"Product created with stock quantity: " + product.getStockQuantity(),
				business
		);

		return product;
	}

	public Product addBarcodeToProduct(Long productId, AddProductBarcodeRequest request, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.ADD_BARCODES);

		Long id = requirePresent(productId, "Product id is required");
		String barcode = normalizeRequired(request.barcode(), "Barcode is required");
		Business business = userService.businessForActor(actorUsername);
		Product product = getProductForStockUpdate(id, business.getId());

		if (product.getStatus() != ProductStatus.CREATED) {
			throw new IllegalArgumentException("Barcodes can only be added to CREATED products");
		}

		long existingBarcodeCount = productBarcodeRepository.countByProduct_Id(id);

		if (existingBarcodeCount >= product.getStockQuantity()) {
			throw new IllegalArgumentException("Product already has barcodes for all stock units");
		}

		if (productBarcodeRepository.existsByBarcode(barcode)) {
			throw new DuplicateBarcodeException(barcode);
		}

		ProductBarcode productBarcode = new ProductBarcode(barcode);
		productBarcode.setReferencee(normalizeOptional(request.referencee()));

		productBarcode.setStatus(product.isReturnableEnabled()
				? ProductBarcodeStatus.NOT_CLAIMED
				: ProductBarcodeStatus.NOT_CLAIMABLE);

		productBarcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.AVAILABLE);

		product.addBarcode(productBarcode);
		productBarcodeRepository.save(productBarcode);

		auditLogService.record(
				"PRODUCT_BARCODE_ADDED",
				"Product",
				String.valueOf(product.getId()),
				actorUsername,
				"Barcode added: " + barcode,
				business
		);

		return productRepository.findWithBarcodesByIdAndBusiness_Id(id, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	public Product updateProductQuantity(
			Long productId,
			UpdateProductQuantityRequest request,
			String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.CREATE_PRODUCTS);

		Long id = requirePresent(productId, "Product id is required");
		int stockQuantity = requireNonNegative(request.stockQuantity(), "Stock quantity cannot be negative");
		Business business = userService.businessForActor(actorUsername);
		Product product = getProductForStockUpdate(id, business.getId());

		long existingBarcodeCount = productBarcodeRepository.countByProduct_Id(id);

		if (stockQuantity < existingBarcodeCount) {
			throw new IllegalArgumentException("Stock quantity cannot be lower than assigned barcode count");
		}

		product.setStockQuantity(stockQuantity);
		product.setStatus(ProductStatus.CREATED);

		Product savedProduct = productRepository.save(product);

		auditLogService.record(
				"PRODUCT_QUANTITY_UPDATED",
				"Product",
				String.valueOf(savedProduct.getId()),
				actorUsername,
				"Product quantity updated to: " + savedProduct.getStockQuantity(),
				business
		);

		return productRepository.findWithBarcodesByIdAndBusiness_Id(id, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	public Product submitProductForApproval(Long productId, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.ADD_BARCODES);

		Long id = requirePresent(productId, "Product id is required");
		Business business = userService.businessForActor(actorUsername);
		Product product = getProductForStockUpdate(id, business.getId());

		long existingBarcodeCount = productBarcodeRepository.countByProduct_Id(id);

		if (existingBarcodeCount != product.getStockQuantity()) {
			throw new IllegalArgumentException("Product barcode count must match stock quantity before approval submission");
		}

		product.setStatus(ProductStatus.PENDING_APPROVAL);

		Product savedProduct = productRepository.save(product);

		auditLogService.record(
				"PRODUCT_SUBMITTED_FOR_APPROVAL",
				"Product",
				String.valueOf(savedProduct.getId()),
				actorUsername,
				"Product submitted for approval with barcode count: " + existingBarcodeCount,
				business
		);

		sendProductApprovalRequestNotification(business, savedProduct, actorUsername, existingBarcodeCount);

		return productRepository.findWithBarcodesByIdAndBusiness_Id(id, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	@Transactional(readOnly = true)
	public List<Product> listProducts() {
		log.debug("products_list_requested mode=all");
		return productRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Page<Product> listProducts(Pageable pageable) {
		return productRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Page<Product> listProducts(Pageable pageable, String actorUsername) {
		Business business = userService.businessForActor(actorUsername);
		return productRepository.findByBusiness_Id(business.getId(), pageable);
	}

	@Transactional(readOnly = true)
	public Product getProductById(Long id) {
		return productRepository.findWithBarcodesById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	@Transactional(readOnly = true)
	public Product getProductById(Long id, String actorUsername) {
		Business business = userService.businessForActor(actorUsername);

		return productRepository.findWithBarcodesByIdAndBusiness_Id(id, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	public Product getProductForStockUpdate(Long id) {
		Product product = productRepository.findLockedById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

		return product;
	}

	public Product getProductForStockUpdate(Long id, Long businessId) {
		Product product = productRepository.findLockedByIdAndBusiness_Id(id, businessId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

		return product;
	}

	@Transactional(readOnly = true)
	public Product getProductByBarcode(String barcode) {
		String normalizedBarcode = normalizeRequired(barcode, "Barcode is required");

		return productBarcodeRepository.findByBarcode(normalizedBarcode)
				.map(ProductBarcode::getProduct)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found for barcode: " + normalizedBarcode));
	}

	@Transactional(readOnly = true)
	public Product getProductByBarcode(String barcode, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.SCAN_BARCODES);

		String normalizedBarcode = normalizeRequired(barcode, "Barcode is required");
		Business business = userService.businessForActor(actorUsername);

		return productBarcodeRepository.findByBarcode(normalizedBarcode, business.getId())
				.map(ProductBarcode::getProduct)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found for barcode: " + normalizedBarcode));
	}

	public Product applyStockMovement(Product product, TransactionType type, int quantity) {
		requirePresent(product, "Product is required");
		requirePresent(type, "Transaction type is required");

		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}

		if (type == TransactionType.BUY) {
			product.setStockQuantity(product.getStockQuantity() + quantity);
		} else if (type == TransactionType.SELL) {
			if (product.getStockQuantity() < quantity) {
				throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
			}

			product.setStockQuantity(product.getStockQuantity() - quantity);
		} else {
			throw new IllegalArgumentException("Unsupported transaction type: " + type);
		}

		if (product.getStockQuantity() < 0) {
			throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
		}

		return productRepository.save(product);
	}

	private String normalizeRequired(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(message);
		}

		return value.trim();
	}

	private String normalizeOptional(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		return value.trim();
	}

	private <T> T requirePresent(T value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}

		return value;
	}

	private int requireNonNegative(Integer value, String message) {
		requirePresent(value, message);

		if (value < 0) {
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

	private BigDecimal moneyWhenEnabled(
			boolean enabled,
			BigDecimal value,
			String requiredMessage,
			String negativeMessage) {
		if (!enabled) {
			return BigDecimal.ZERO;
		}

		return requireNonNegative(requirePresent(value, requiredMessage), negativeMessage);
	}

	private void sendProductApprovalRequestNotification(
			Business business,
			Product product,
			String actorUsername,
			long barcodeCount) {
		try {
			appEmailService.sendProductApprovalRequestEmail(business, product, actorUsername, barcodeCount);
		} catch (RuntimeException exception) {
			log.warn(
					"product_approval_request_email_failed_non_blocking recipient={} businessId={} productId={} actor={} reason={}",
					AppEmailService.maskEmail(business.getOwner().getEmailAddress()),
					business.getId(),
					product.getId(),
					actorUsername,
					exception.getMessage());
		}
	}
}
