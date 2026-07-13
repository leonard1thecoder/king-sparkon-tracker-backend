package com.king_sparkon_tracker.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.CreateTuckShopPurchaseRequest;
import com.king_sparkon_tracker.backend.dto.ProductBarcodeModeRequest;
import com.king_sparkon_tracker.backend.dto.ProductBarcodeModeResponse;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseItemRequest;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseResponse;
import com.king_sparkon_tracker.backend.exception.DuplicateBarcodeException;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessFeature;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.ProductBarcodeConfiguration;
import com.king_sparkon_tracker.backend.model.ProductBarcodeMode;
import com.king_sparkon_tracker.backend.model.ProductBarcodeStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TuckShopFulfilmentStatus;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeConfigurationRepository;
import com.king_sparkon_tracker.backend.repository.ProductBarcodeRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;

@Service
@Transactional
public class ProductBarcodeAutomationService {

	private final ProductRepository productRepository;
	private final ProductBarcodeRepository barcodeRepository;
	private final ProductBarcodeConfigurationRepository configurationRepository;
	private final InventoryTransactionRepository transactionRepository;
	private final TrackerUserService userService;
	private final BusinessAccessService businessAccessService;
	private final TuckShopService tuckShopService;
	private final AuditLogService auditLogService;

	public ProductBarcodeAutomationService(
			ProductRepository productRepository,
			ProductBarcodeRepository barcodeRepository,
			ProductBarcodeConfigurationRepository configurationRepository,
			InventoryTransactionRepository transactionRepository,
			TrackerUserService userService,
			BusinessAccessService businessAccessService,
			TuckShopService tuckShopService,
			AuditLogService auditLogService) {
		this.productRepository = productRepository;
		this.barcodeRepository = barcodeRepository;
		this.configurationRepository = configurationRepository;
		this.transactionRepository = transactionRepository;
		this.userService = userService;
		this.businessAccessService = businessAccessService;
		this.tuckShopService = tuckShopService;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<ProductBarcodeModeResponse> listConfigurations(String actorUsername) {
		Business business = userService.businessForActor(actorUsername);
		return productRepository.findByBusiness_Id(business.getId(), Pageable.unpaged()).getContent().stream()
				.map(product -> ProductBarcodeModeResponse.from(product, modeFor(product)))
				.toList();
	}

	public ProductBarcodeModeResponse configure(Long productId, ProductBarcodeModeRequest request, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.CREATE_PRODUCTS);
		Business business = userService.businessForActor(actorUsername);
		Product product = productRepository.findWithBarcodesByIdAndBusiness_Id(productId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
		ProductBarcodeMode mode = Objects.requireNonNull(request.barcodeMode(), "Barcode mode is required");

		if (mode == ProductBarcodeMode.BRANDED) {
			String manufacturerBarcode = required(request.manufacturerBarcode(), "Manufacturer barcode is required for a barcoded brand");
			if (!manufacturerBarcode.equals(product.getProductBarcode())
					&& productRepository.existsByBusiness_IdAndProductBarcode(business.getId(), manufacturerBarcode)) {
				throw new DuplicateBarcodeException(manufacturerBarcode);
			}
			product.setProductBarcode(manufacturerBarcode);
		} else {
			product.setProductBarcode(null);
			product.setBarcodeCatalog(null);
		}

		ProductBarcodeConfiguration configuration = configurationRepository.findById(productId)
				.orElseGet(() -> new ProductBarcodeConfiguration(product, mode));
		configuration.setBarcodeMode(mode);
		productRepository.save(product);
		configurationRepository.save(configuration);

		auditLogService.record(
				"PRODUCT_BARCODE_MODE_CONFIGURED",
				"Product",
				String.valueOf(productId),
				actorUsername,
				"Barcode mode changed to " + mode,
				business);
		return ProductBarcodeModeResponse.from(product, mode);
	}

	public ProductBarcodeModeResponse fillAutomaticStock(Long productId, String actorUsername) {
		businessAccessService.requireFeature(actorUsername, BusinessFeature.ADD_BARCODES);
		Business business = userService.businessForActor(actorUsername);
		Product product = productRepository.findWithBarcodesByIdAndBusiness_Id(productId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
		requireAutomatic(product);
		int created = ensureAutomaticUnits(product, product.getStockQuantity());
		auditLogService.record(
				"PRODUCT_AUTOMATIC_BARCODES_FILLED",
				"Product",
				String.valueOf(productId),
				actorUsername,
				"Automatically created " + created + " stock-unit barcodes",
				business);
		return ProductBarcodeModeResponse.from(reload(productId, business.getId()), ProductBarcodeMode.AUTO_GENERATED);
	}

	public TuckShopPurchaseResponse createWorkerPurchase(CreateTuckShopPurchaseRequest request, String actorUsername) {
		TrackerUser worker = requireWorker(actorUsername);
		Business business = requireBusiness(worker);
		List<TuckShopPurchaseItemRequest> preparedItems = new ArrayList<>();

		for (TuckShopPurchaseItemRequest item : request.items()) {
			Product product = productRepository.findWithBarcodesByIdAndBusiness_Id(item.productId(), business.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.productId()));
			if (modeFor(product) == ProductBarcodeMode.AUTO_GENERATED) {
				ensureAutomaticUnits(product, product.getStockQuantity());
				List<String> unitCodes = availableUnits(product.getId(), item.quantity());
				if (unitCodes.size() != item.quantity()) {
					throw new IllegalArgumentException("Not enough automatically generated stock units for " + product.getName());
				}
				preparedItems.add(new TuckShopPurchaseItemRequest(item.productId(), item.quantity(), unitCodes));
			} else {
				preparedItems.add(item);
			}
		}

		CreateTuckShopPurchaseRequest prepared = new CreateTuckShopPurchaseRequest(
				request.paymentEmail(),
				request.paymentContact(),
				request.workerId(),
				request.tipAmount(),
				request.tipCallbackUrl(),
				preparedItems,
				request.paymentType(),
				request.customerUsername());
		return tuckShopService.createWorkerBarcodePurchase(prepared, actorUsername);
	}

	public TuckShopPurchaseResponse prepareAutomaticOnlineLine(
			Long transactionId,
			Long productId,
			String actorUsername) {
		TrackerUser worker = requireWorker(actorUsername);
		Business business = requireBusiness(worker);
		InventoryTransaction transaction = transactionRepository.findByIdAndBusiness_Id(transactionId, business.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Online purchase not found: " + transactionId));
		requirePaidPreparation(transaction);

		TransactionItem item = transaction.getItems().stream()
				.filter(candidate -> Objects.equals(candidate.getProduct().getId(), productId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Product is not part of this purchase: " + productId));
		Product product = item.getProduct();
		requireAutomatic(product);

		int outstanding = Math.max(item.getQuantity() - item.getBarcodes().size(), 0);
		List<ProductBarcode> available = barcodeRepository.findByProduct_IdOrderByIdAsc(productId).stream()
				.filter(barcode -> barcode.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.AVAILABLE)
				.limit(outstanding)
				.toList();
		if (available.size() != outstanding) {
			throw new IllegalStateException("Not enough generated stock units remain for " + product.getName());
		}

		for (ProductBarcode barcode : available) {
			item.addBarcode(barcode.getUnitCode());
			barcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.SOLD);
			if (product.isReturnableEnabled()) barcode.setReferenceEmail(transaction.getPaymentEmail());
		}
		barcodeRepository.saveAll(available);
		if (transaction.getOutstandingBarcodeCount() == 0) transaction.markReadyForCollection(worker.getId());
		InventoryTransaction saved = transactionRepository.save(transaction);

		auditLogService.record(
				"ONLINE_PURCHASE_AUTOMATIC_BARCODES_ASSIGNED",
				"InventoryTransaction",
				String.valueOf(saved.getId()),
				actorUsername,
				"Automatically assigned " + available.size() + " generated stock-unit barcodes",
				business);
		return TuckShopPurchaseResponse.from(saved, null);
	}

	@Transactional(readOnly = true)
	public List<TuckShopPurchaseResponse> listCompletedWorkerSales(String actorUsername) {
		TrackerUser worker = requireWorker(actorUsername);
		Business business = requireBusiness(worker);
		return transactionRepository.findByBusiness_Id(business.getId(), Pageable.unpaged()).getContent().stream()
				.filter(transaction -> transaction.getFulfilmentStatus() == TuckShopFulfilmentStatus.COLLECTED
						|| transaction.getPaymentStatus() == TransactionPaymentStatus.PAID)
				.sorted(Comparator.comparing(InventoryTransaction::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
				.map(transaction -> TuckShopPurchaseResponse.from(transaction, null))
				.toList();
	}

	public ProductBarcodeMode modeFor(Product product) {
		return configurationRepository.findById(product.getId())
				.map(ProductBarcodeConfiguration::getBarcodeMode)
				.orElseGet(() -> StringUtils.hasText(product.getProductBarcode())
						? ProductBarcodeMode.BRANDED
						: ProductBarcodeMode.AUTO_GENERATED);
	}

	private int ensureAutomaticUnits(Product product, int targetCount) {
		requireAutomatic(product);
		long existing = barcodeRepository.countByProduct_Id(product.getId());
		int missing = Math.max(targetCount - Math.toIntExact(existing), 0);
		List<ProductBarcode> created = new ArrayList<>();
		for (int index = 0; index < missing; index++) {
			String unitCode = uniqueUnitCode();
			ProductBarcode barcode = new ProductBarcode(unitCode, unitCode);
			barcode.setStatus(product.isReturnableEnabled() ? ProductBarcodeStatus.NOT_CLAIMED : ProductBarcodeStatus.NOT_CLAIMABLE);
			barcode.setAvailabilityStatus(ProductBarcodeAvailabilityStatus.AVAILABLE);
			product.addBarcode(barcode);
			created.add(barcode);
		}
		if (!created.isEmpty()) barcodeRepository.saveAll(created);
		return created.size();
	}

	private List<String> availableUnits(Long productId, int quantity) {
		return barcodeRepository.findByProduct_IdOrderByIdAsc(productId).stream()
				.filter(barcode -> barcode.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.AVAILABLE)
				.map(ProductBarcode::getUnitCode)
				.limit(quantity)
				.toList();
	}

	private String uniqueUnitCode() {
		String unitCode;
		do {
			unitCode = ProductBarcode.generateUnitCode();
		} while (barcodeRepository.existsByUnitCode(unitCode));
		return unitCode;
	}

	private Product reload(Long productId, Long businessId) {
		return productRepository.findWithBarcodesByIdAndBusiness_Id(productId, businessId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
	}

	private void requireAutomatic(Product product) {
		if (modeFor(product) != ProductBarcodeMode.AUTO_GENERATED) {
			throw new IllegalArgumentException("This action is only available for products without a manufacturer barcode");
		}
	}

	private void requirePaidPreparation(InventoryTransaction transaction) {
		if (transaction.getPaymentStatus() != TransactionPaymentStatus.PAID) {
			throw new IllegalStateException("Only paid online purchases can be prepared");
		}
		if (transaction.getFulfilmentStatus() != TuckShopFulfilmentStatus.AWAITING_BARCODE_ASSIGNMENT
				&& transaction.getFulfilmentStatus() != TuckShopFulfilmentStatus.READY_FOR_COLLECTION) {
			throw new IllegalStateException("This transaction is not waiting for collection preparation");
		}
	}

	private TrackerUser requireWorker(String actorUsername) {
		TrackerUser worker = userService.getUserByUsername(required(actorUsername, "Authenticated username is required"));
		if (worker.getPrivilege() == null || worker.getPrivilege().getName() != PrivilegeRole.Worker) {
			throw new IllegalArgumentException("Only workers can use automatic product checkout");
		}
		return worker;
	}

	private Business requireBusiness(TrackerUser worker) {
		if (worker.getBusiness() == null) throw new IllegalArgumentException("Worker must belong to a business");
		return worker.getBusiness();
	}

	private String required(String value, String message) {
		if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
		return value.trim();
	}
}
