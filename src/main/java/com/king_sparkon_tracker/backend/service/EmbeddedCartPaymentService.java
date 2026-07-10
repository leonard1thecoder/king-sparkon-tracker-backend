package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.CreateTuckShopPurchaseRequest;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.CreateRequest;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.CreateResponse;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.StatusResponse;
import com.king_sparkon_tracker.backend.dto.EmbeddedCartPaymentDtos.TicketItem;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseItemRequest;
import com.king_sparkon_tracker.backend.dto.TuckShopPurchaseResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductBarcodeAvailabilityStatus;
import com.king_sparkon_tracker.backend.model.ProductStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;
import com.king_sparkon_tracker.backend.service.StripeService.CreatedEmbeddedPaymentIntent;
import com.king_sparkon_tracker.backend.tickets.config.TicketProperties;
import com.king_sparkon_tracker.backend.tickets.domain.TicketBusinessRules;
import com.king_sparkon_tracker.backend.tickets.model.EventTicketType;
import com.king_sparkon_tracker.backend.tickets.model.TicketEvent;
import com.king_sparkon_tracker.backend.tickets.model.TicketEventStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketPayment;
import com.king_sparkon_tracker.backend.tickets.model.TicketPaymentStatus;
import com.king_sparkon_tracker.backend.tickets.model.TicketType;
import com.king_sparkon_tracker.backend.tickets.repository.TicketEventRepository;
import com.king_sparkon_tracker.backend.tickets.repository.TicketPaymentRepository;
import com.stripe.model.PaymentIntent;

@Service
@Transactional
public class EmbeddedCartPaymentService {

	private static final String CURRENCY = "ZAR";
	private static final int MAX_CART_LINES = 20;

	private final StripeService stripeService;
	private final ProductRepository productRepository;
	private final ProductPricingService productPricingService;
	private final TuckShopService tuckShopService;
	private final TransactionService transactionService;
	private final InventoryTransactionRepository transactionRepository;
	private final EmbeddedTicketFulfilmentService embeddedTicketFulfilmentService;
	private final TicketEventRepository ticketEventRepository;
	private final TicketPaymentRepository ticketPaymentRepository;
	private final TicketProperties ticketProperties;
	private final TrackerUserService trackerUserService;

	public EmbeddedCartPaymentService(
			StripeService stripeService,
			ProductRepository productRepository,
			ProductPricingService productPricingService,
			TuckShopService tuckShopService,
			TransactionService transactionService,
			InventoryTransactionRepository transactionRepository,
			EmbeddedTicketFulfilmentService embeddedTicketFulfilmentService,
			TicketEventRepository ticketEventRepository,
			TicketPaymentRepository ticketPaymentRepository,
			TicketProperties ticketProperties,
			TrackerUserService trackerUserService) {
		this.stripeService = stripeService;
		this.productRepository = productRepository;
		this.productPricingService = productPricingService;
		this.tuckShopService = tuckShopService;
		this.transactionService = transactionService;
		this.transactionRepository = transactionRepository;
		this.embeddedTicketFulfilmentService = embeddedTicketFulfilmentService;
		this.ticketEventRepository = ticketEventRepository;
		this.ticketPaymentRepository = ticketPaymentRepository;
		this.ticketProperties = ticketProperties;
		this.trackerUserService = trackerUserService;
	}

	@Transactional(readOnly = true)
	public CreateResponse create(CreateRequest request, String actorUsername) {
		if (request.empty()) {
			throw new IllegalArgumentException("Cart must contain at least one product or ticket");
		}
		int lineCount = safeProducts(request).size() + safeTickets(request).size();
		if (lineCount > MAX_CART_LINES) {
			throw new IllegalArgumentException("Cart cannot contain more than " + MAX_CART_LINES + " payment lines");
		}

		TrackerUser actor = trackerUserService.getUserByUsername(actorUsername);
		BigDecimal amount = calculateTrustedAmount(request);
		Map<String, String> metadata = paymentMetadata(request, actorUsername, actor);
		CreatedEmbeddedPaymentIntent intent = stripeService.createEmbeddedCartPaymentIntent(
				amount,
				request.buyerEmail(),
				metadata,
				"cart:" + actor.getId() + ":" + request.idempotencyKey());

		return new CreateResponse(intent.paymentIntentId(), intent.clientSecret(), amount, CURRENCY, intent.status());
	}

	@Transactional(readOnly = true)
	public StatusResponse status(String paymentIntentId, String actorUsername) {
		PaymentIntent intent = stripeService.retrievePaymentIntent(paymentIntentId);
		Map<String, String> metadata = intent.getMetadata();
		requireActor(metadata, actorUsername);

		FulfilmentSnapshot snapshot = fulfilmentSnapshot(metadata, paymentIntentId);
		boolean fulfilled = "succeeded".equalsIgnoreCase(intent.getStatus()) && snapshot.complete();
		List<TuckShopPurchaseResponse> productPurchases = snapshot.transactions().stream()
				.map(transaction -> TuckShopPurchaseResponse.from(transaction, null, null))
				.toList();
		List<String> ticketPaymentIds = snapshot.ticketPayments().stream().map(TicketPayment::getId).toList();
		BigDecimal amount = BigDecimal.valueOf(intent.getAmount()).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
		String message = fulfilled
				? "Payment verified and cart fulfilled"
				: "Payment is " + intent.getStatus() + "; waiting for verified webhook fulfilment";

		return new StatusResponse(
				intent.getId(),
				amount,
				intent.getCurrency() == null ? CURRENCY : intent.getCurrency().toUpperCase(),
				intent.getStatus(),
				fulfilled,
				productPurchases,
				ticketPaymentIds,
				message);
	}

	public void handlePaymentIntentSucceeded(PaymentIntent intent, String stripeEventId) {
		Map<String, String> metadata = new LinkedHashMap<>(intent.getMetadata());
		if (!"true".equalsIgnoreCase(metadata.get("embeddedCart"))) {
			return;
		}

		String actorUsername = required(metadata, "actorUsername");
		String buyerName = required(metadata, "buyerName");
		String buyerEmail = required(metadata, "buyerEmail");
		TrackerUser actor = trackerUserService.getUserByUsername(actorUsername);
		FulfilmentSnapshot existing = fulfilmentSnapshot(metadata, intent.getId());

		if (existing.complete()) {
			updateFulfilmentMetadata(intent.getId(), existing.transactionIds(), existing.ticketPaymentIds());
			return;
		}
		if (existing.hasAnyRecords()) {
			throw new IllegalStateException("Partial embedded cart fulfilment detected for PaymentIntent " + intent.getId());
		}

		List<Long> transactionIds = fulfilProducts(metadata, actorUsername, buyerEmail, intent.getId(), stripeEventId);
		List<String> ticketPaymentIds = fulfilTickets(metadata, actor, buyerName, buyerEmail, intent.getId());
		updateFulfilmentMetadata(intent.getId(), transactionIds, ticketPaymentIds);
	}

	private void updateFulfilmentMetadata(String paymentIntentId, List<Long> transactionIds, List<String> ticketPaymentIds) {
		Map<String, String> fulfilmentMetadata = new LinkedHashMap<>();
		fulfilmentMetadata.put("fulfilled", "true");
		fulfilmentMetadata.put("fulfilledAt", Instant.now().toString());
		fulfilmentMetadata.put("transactionIds", join(transactionIds));
		fulfilmentMetadata.put("ticketPaymentIds", String.join(",", ticketPaymentIds));
		stripeService.updatePaymentIntentMetadata(paymentIntentId, fulfilmentMetadata);
	}

	private FulfilmentSnapshot fulfilmentSnapshot(Map<String, String> metadata, String paymentIntentId) {
		int expectedTransactions = expectedProductTransactionCount(metadata);
		int expectedTicketPayments = expectedTicketPaymentCount(metadata);
		List<InventoryTransaction> transactions = transactionRepository.findByPaymentReferenceOrderByIdAsc(paymentIntentId);
		List<TicketPayment> ticketPayments = ticketPaymentRepository.findAllByPaymentReferenceOrderByCreatedAtAsc(paymentIntentId);
		return new FulfilmentSnapshot(expectedTransactions, expectedTicketPayments, transactions, ticketPayments);
	}

	private int expectedProductTransactionCount(Map<String, String> metadata) {
		String configured = metadata.get("expectedProductTransactions");
		if (configured != null && !configured.isBlank()) {
			return intValue(configured, "Invalid expected product transaction count");
		}
		return expectedProductTransactionCount(productItems(metadata));
	}

	private int expectedProductTransactionCount(List<TuckShopPurchaseItemRequest> items) {
		return (int) items.stream()
				.map(item -> requireBusinessId(requireProduct(item.productId())))
				.distinct()
				.count();
	}

	private int expectedTicketPaymentCount(Map<String, String> metadata) {
		String configured = metadata.get("expectedTicketPayments");
		return configured == null || configured.isBlank()
				? ticketItems(metadata).size()
				: intValue(configured, "Invalid expected ticket payment count");
	}

	private List<Long> fulfilProducts(
			Map<String, String> metadata,
			String actorUsername,
			String buyerEmail,
			String paymentIntentId,
			String stripeEventId) {
		Map<Long, List<TuckShopPurchaseItemRequest>> byBusiness = new LinkedHashMap<>();
		for (TuckShopPurchaseItemRequest item : productItems(metadata)) {
			Product product = requireProduct(item.productId());
			Long businessId = requireBusinessId(product);
			byBusiness.computeIfAbsent(businessId, ignored -> new ArrayList<>()).add(item);
		}

		List<Long> transactionIds = new ArrayList<>();
		for (List<TuckShopPurchaseItemRequest> items : byBusiness.values()) {
			TuckShopPurchaseResponse purchase = tuckShopService.createEmbeddedPaymentPurchase(
					new CreateTuckShopPurchaseRequest(buyerEmail, buyerEmail, null, null, null, items),
					actorUsername,
					paymentIntentId);
			transactionService.handleWebsitePaymentSucceeded(purchase.transactionId(), paymentIntentId, stripeEventId);
			transactionIds.add(purchase.transactionId());
		}
		return transactionIds;
	}

	private List<String> fulfilTickets(
			Map<String, String> metadata,
			TrackerUser actor,
			String buyerName,
			String buyerEmail,
			String paymentIntentId) {
		List<String> paymentIds = new ArrayList<>();
		for (TicketItem item : ticketItems(metadata)) {
			paymentIds.add(embeddedTicketFulfilmentService.fulfil(
					item,
					actor,
					buyerName,
					buyerEmail,
					paymentIntentId));
		}
		return paymentIds;
	}

	private BigDecimal calculateTrustedAmount(CreateRequest request) {
		BigDecimal total = BigDecimal.ZERO;
		for (TuckShopPurchaseItemRequest item : safeProducts(request)) {
			Product product = requireProduct(item.productId());
			int quantity = requirePositive(item.quantity());
			requireProductReadyForPayment(product, quantity);
			total = total.add(productPricingService.priceForSale(product).multiply(BigDecimal.valueOf(quantity)));
		}
		for (TicketItem item : safeTickets(request)) {
			total = total.add(quoteTicket(item));
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private void requireProductReadyForPayment(Product product, int quantity) {
		if (product.getStatus() != ProductStatus.CREATED || product.getStockQuantity() < quantity) {
			throw new IllegalArgumentException("Product is unavailable or has insufficient stock: " + product.getName());
		}
		requireBusinessId(product);
		long availableUnits = product.getBarcodes().stream()
				.filter(barcode -> barcode.getAvailabilityStatus() == ProductBarcodeAvailabilityStatus.AVAILABLE)
				.count();
		if (availableUnits < quantity) {
			throw new IllegalArgumentException("Product does not have enough available barcode units: " + product.getName());
		}
	}

	private Long requireBusinessId(Product product) {
		if (product.getBusiness() == null || product.getBusiness().getId() == null || product.getBusiness().getOwner() == null) {
			throw new IllegalArgumentException("Product must belong to a business with an owner: " + product.getName());
		}
		return product.getBusiness().getId();
	}

	private BigDecimal quoteTicket(TicketItem item) {
		TicketEvent event = ticketEventRepository.findById(item.eventId())
				.orElseThrow(() -> new ResourceNotFoundException("Ticket event not found: " + item.eventId()));
		if (event.getStatus() != TicketEventStatus.PUBLISHED) {
			throw new IllegalArgumentException("Ticket event is not published: " + event.getName());
		}
		EventTicketType ticketType = event.getTicketTypes().stream()
				.filter(candidate -> candidate.getType() == item.ticketType())
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Ticket type is unavailable for event: " + event.getName()));
		TicketBusinessRules.requirePurchaseCapacity(ticketType.getCapacity(), ticketType.getSold(), item.quantity());
		BigDecimal subtotal = TicketBusinessRules.money(ticketType.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
		BigDecimal fee = TicketBusinessRules.calculatePercentAmount(subtotal, ticketProperties.checkoutServiceFeePercent());
		return TicketBusinessRules.money(subtotal.add(fee));
	}

	private Map<String, String> paymentMetadata(CreateRequest request, String actorUsername, TrackerUser actor) {
		Map<String, String> metadata = new LinkedHashMap<>();
		metadata.put("embeddedCart", "true");
		metadata.put("fulfilled", "false");
		metadata.put("actorUsername", actorUsername);
		metadata.put("actorId", String.valueOf(actor.getId()));
		metadata.put("buyerName", request.buyerName().trim());
		metadata.put("buyerEmail", request.buyerEmail().trim().toLowerCase());
		metadata.put("expectedProductTransactions", String.valueOf(expectedProductTransactionCount(safeProducts(request))));
		metadata.put("expectedTicketPayments", String.valueOf(safeTickets(request).size()));
		metadata.put("productCount", String.valueOf(safeProducts(request).size()));
		for (int index = 0; index < safeProducts(request).size(); index++) {
			TuckShopPurchaseItemRequest item = safeProducts(request).get(index);
			metadata.put("product" + index, item.productId() + ":" + item.quantity());
		}
		metadata.put("ticketCount", String.valueOf(safeTickets(request).size()));
		for (int index = 0; index < safeTickets(request).size(); index++) {
			TicketItem item = safeTickets(request).get(index);
			metadata.put("ticket" + index, item.eventId() + "|" + item.ticketType().name() + "|" + item.quantity());
		}
		return metadata;
	}

	private List<TuckShopPurchaseItemRequest> productItems(Map<String, String> metadata) {
		int count = intValue(metadata.getOrDefault("productCount", "0"), "Invalid product count");
		List<TuckShopPurchaseItemRequest> items = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			String[] parts = required(metadata, "product" + index).split(":", 2);
			if (parts.length != 2) throw new IllegalArgumentException("Invalid product payment metadata");
			items.add(new TuckShopPurchaseItemRequest(Long.valueOf(parts[0]), Integer.valueOf(parts[1]), List.of()));
		}
		return items;
	}

	private List<TicketItem> ticketItems(Map<String, String> metadata) {
		int count = intValue(metadata.getOrDefault("ticketCount", "0"), "Invalid ticket count");
		List<TicketItem> items = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			String[] parts = required(metadata, "ticket" + index).split("\\|", 3);
			if (parts.length != 3) throw new IllegalArgumentException("Invalid ticket payment metadata");
			items.add(new TicketItem(parts[0], TicketType.valueOf(parts[1]), Integer.parseInt(parts[2])));
		}
		return items;
	}

	private Product requireProduct(Long productId) {
		return productRepository.findWithBarcodesById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
	}

	private void requireActor(Map<String, String> metadata, String actorUsername) {
		if (!actorUsername.equals(metadata.get("actorUsername"))) {
			throw new IllegalArgumentException("Payment does not belong to the authenticated user");
		}
	}

	private String required(Map<String, String> metadata, String name) {
		String value = metadata.get(name);
		if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing Stripe metadata: " + name);
		return value;
	}

	private int intValue(String value, String message) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(message, exception);
		}
	}

	private int requirePositive(Integer value) {
		if (value == null || value <= 0) throw new IllegalArgumentException("Cart quantity must be greater than zero");
		return value;
	}

	private List<TuckShopPurchaseItemRequest> safeProducts(CreateRequest request) {
		return request.products() == null ? List.of() : request.products();
	}

	private List<TicketItem> safeTickets(CreateRequest request) {
		return request.tickets() == null ? List.of() : request.tickets();
	}

	private String join(List<Long> values) {
		return values.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
	}

	private record FulfilmentSnapshot(
			int expectedTransactions,
			int expectedTicketPayments,
			List<InventoryTransaction> transactions,
			List<TicketPayment> ticketPayments) {

		boolean complete() {
			return transactions.size() == expectedTransactions
					&& ticketPayments.size() == expectedTicketPayments
					&& transactions.stream().allMatch(transaction -> transaction.getPaymentStatus() == TransactionPaymentStatus.PAID)
					&& ticketPayments.stream().allMatch(payment -> payment.getStatus() == TicketPaymentStatus.SUCCESS);
		}

		boolean hasAnyRecords() {
			return !transactions.isEmpty() || !ticketPayments.isEmpty();
		}

		List<Long> transactionIds() {
			return transactions.stream().map(InventoryTransaction::getId).toList();
		}

		List<String> ticketPaymentIds() {
			return ticketPayments.stream().map(TicketPayment::getId).toList();
		}
	}
}
