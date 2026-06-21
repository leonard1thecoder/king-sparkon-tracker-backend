package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.AlcoholReportResponse;
import com.king_sparkon_tracker.backend.dto.InventorySummaryResponse;
import com.king_sparkon_tracker.backend.dto.ProductMovementResponse;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.ProductBarcode;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.ProductRepository;
import com.king_sparkon_tracker.backend.repository.TransactionItemRepository;

@Service
@Transactional(readOnly = true)
public class ReportService {

	private static final Logger log = LoggerFactory.getLogger(ReportService.class);

	private static final LocalDateTime DEFAULT_FROM = LocalDateTime.of(1970, 1, 1, 0, 0);

	private final TransactionItemRepository transactionItemRepository;
	private final ProductRepository productRepository;
	private final TrackerUserService userService;

	public ReportService(
			TransactionItemRepository transactionItemRepository,
			ProductRepository productRepository,
			TrackerUserService userService) {
		this.transactionItemRepository = transactionItemRepository;
		this.productRepository = productRepository;
		this.userService = userService;
	}

	/**
	 * Calculates alcohol-only bought and sold quantities and values over a validated date range.
	 */
	public AlcoholReportResponse alcoholReport(LocalDateTime from, LocalDateTime to) {
		LocalDateTime start = from == null ? DEFAULT_FROM : from;
		LocalDateTime end = to == null ? LocalDateTime.now() : to;
		if (start.isAfter(end)) {
			log.warn("alcohol_report_rejected reason=invalid_date_range from={} to={}", start, end);
			throw new IllegalArgumentException("Report from date cannot be after to date");
		}

		List<TransactionItem> alcoholItems = transactionItemRepository
				.findByTransaction_DateBetweenAndProduct_Category(start, end, ProductCategory.Alcohol);
		List<MovementLine> alcoholLines = movementLinesFrom(alcoholItems);

		int boughtQuantity = quantityFor(alcoholLines, TransactionType.BUY);
		int soldQuantity = quantityFor(alcoholLines, TransactionType.SELL);
		BigDecimal boughtValue = valueFor(alcoholLines, TransactionType.BUY);
		BigDecimal soldValue = valueFor(alcoholLines, TransactionType.SELL);
		log.info(
				"alcohol_report_generated from={} to={} boughtQuantity={} soldQuantity={}",
				start,
				end,
				boughtQuantity,
				soldQuantity);
		return new AlcoholReportResponse(start, end, boughtQuantity, soldQuantity, boughtValue, soldValue);
	}

	/**
	 * Calculates alcohol movement for one business over a validated date range.
	 */
	public AlcoholReportResponse alcoholReport(LocalDateTime from, LocalDateTime to, String actorUsername) {
		Business business = userService.businessForActor(actorUsername);
		LocalDateTime start = from == null ? DEFAULT_FROM : from;
		LocalDateTime end = to == null ? LocalDateTime.now() : to;
		if (start.isAfter(end)) {
			log.warn(
					"alcohol_report_rejected reason=invalid_date_range businessId={} from={} to={}",
					business.getId(),
					start,
					end);
			throw new IllegalArgumentException("Report from date cannot be after to date");
		}

		List<TransactionItem> alcoholItems = transactionItemRepository
				.findByTransaction_Business_IdAndTransaction_DateBetweenAndProduct_Category(
						business.getId(),
						start,
						end,
						ProductCategory.Alcohol);
		List<MovementLine> alcoholLines = movementLinesFrom(alcoholItems);

		int boughtQuantity = quantityFor(alcoholLines, TransactionType.BUY);
		int soldQuantity = quantityFor(alcoholLines, TransactionType.SELL);
		BigDecimal boughtValue = valueFor(alcoholLines, TransactionType.BUY);
		BigDecimal soldValue = valueFor(alcoholLines, TransactionType.SELL);
		log.info(
				"alcohol_report_generated businessId={} from={} to={} boughtQuantity={} soldQuantity={}",
				business.getId(),
				start,
				end,
				boughtQuantity,
				soldQuantity);
		return new AlcoholReportResponse(start, end, boughtQuantity, soldQuantity, boughtValue, soldValue);
	}

	/**
	 * Summarizes stock counts and valuation with a caller-controlled low-stock threshold.
	 */
	public InventorySummaryResponse inventorySummary(int lowStockThreshold) {
		if (lowStockThreshold < 0) {
			log.warn("inventory_summary_rejected reason=negative_threshold threshold={}", lowStockThreshold);
			throw new IllegalArgumentException("Low stock threshold cannot be negative");
		}
		List<StockLine> stockLines = productRepository.findAll().stream()
				.map(product -> new StockLine(product.getCategory(), product.getPrice(), product.getStockQuantity()))
				.toList();
		BigDecimal totalStockValue = stockLines.parallelStream()
				.map(line -> line.price().multiply(BigDecimal.valueOf(line.stockQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int totalStockQuantity = stockLines.parallelStream()
				.mapToInt(StockLine::stockQuantity)
				.sum();
		InventorySummaryResponse summary = new InventorySummaryResponse(
				stockLines.size(),
				stockLines.parallelStream().filter(line -> line.category() == ProductCategory.Alcohol).count(),
				stockLines.parallelStream().filter(line -> line.category() == ProductCategory.NonAlcohol).count(),
				totalStockQuantity,
				totalStockValue,
				stockLines.parallelStream().filter(line -> line.stockQuantity() <= lowStockThreshold).count());
		log.info(
				"inventory_summary_generated totalProducts={} totalStockQuantity={} lowStockThreshold={}",
				summary.totalProducts(),
				summary.totalStockQuantity(),
				lowStockThreshold);
		return summary;
	}

	/**
	 * Summarizes stock counts and valuation for one business.
	 */
	public InventorySummaryResponse inventorySummary(int lowStockThreshold, String actorUsername) {
		if (lowStockThreshold < 0) {
			log.warn("inventory_summary_rejected reason=negative_threshold threshold={}", lowStockThreshold);
			throw new IllegalArgumentException("Low stock threshold cannot be negative");
		}
		Business business = userService.businessForActor(actorUsername);
		List<StockLine> stockLines = productRepository.findByBusiness_Id(business.getId()).stream()
				.map(product -> new StockLine(product.getCategory(), product.getPrice(), product.getStockQuantity()))
				.toList();
		BigDecimal totalStockValue = stockLines.parallelStream()
				.map(line -> line.price().multiply(BigDecimal.valueOf(line.stockQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		int totalStockQuantity = stockLines.parallelStream()
				.mapToInt(StockLine::stockQuantity)
				.sum();
		InventorySummaryResponse summary = new InventorySummaryResponse(
				stockLines.size(),
				stockLines.parallelStream().filter(line -> line.category() == ProductCategory.Alcohol).count(),
				stockLines.parallelStream().filter(line -> line.category() == ProductCategory.NonAlcohol).count(),
				totalStockQuantity,
				totalStockValue,
				stockLines.parallelStream().filter(line -> line.stockQuantity() <= lowStockThreshold).count());
		log.info(
				"inventory_summary_generated businessId={} totalProducts={} totalStockQuantity={} lowStockThreshold={}",
				business.getId(),
				summary.totalProducts(),
				summary.totalStockQuantity(),
				lowStockThreshold);
		return summary;
	}

	/**
	 * Groups transaction items by product and ranks product movement by sold quantity.
	 */
	public List<ProductMovementResponse> productMovementReport(LocalDateTime from, LocalDateTime to) {
		LocalDateTime start = from == null ? DEFAULT_FROM : from;
		LocalDateTime end = to == null ? LocalDateTime.now() : to;
		if (start.isAfter(end)) {
			log.warn("product_movement_report_rejected reason=invalid_date_range from={} to={}", start, end);
			throw new IllegalArgumentException("Report from date cannot be after to date");
		}

		ConcurrentMap<ProductMovementKey, List<MovementLine>> byProduct = movementLinesFrom(
				transactionItemRepository.findByTransaction_DateBetween(start, end)).parallelStream()
				.collect(Collectors.groupingByConcurrent(MovementLine::productKey));

		List<ProductMovementResponse> report = byProduct.values().parallelStream()
				.map(this::movementForProduct)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(ProductMovementResponse::soldQuantity).reversed())
				.toList();
		log.info("product_movement_report_generated from={} to={} products={}", start, end, report.size());
		return report;
	}

	/**
	 * Groups transaction items by product within one business.
	 */
	public List<ProductMovementResponse> productMovementReport(
			LocalDateTime from,
			LocalDateTime to,
			String actorUsername) {
		Business business = userService.businessForActor(actorUsername);
		LocalDateTime start = from == null ? DEFAULT_FROM : from;
		LocalDateTime end = to == null ? LocalDateTime.now() : to;
		if (start.isAfter(end)) {
			log.warn(
					"product_movement_report_rejected reason=invalid_date_range businessId={} from={} to={}",
					business.getId(),
					start,
					end);
			throw new IllegalArgumentException("Report from date cannot be after to date");
		}

		ConcurrentMap<ProductMovementKey, List<MovementLine>> byProduct = movementLinesFrom(
				transactionItemRepository.findByTransaction_Business_IdAndTransaction_DateBetween(
						business.getId(),
						start,
						end)).parallelStream()
				.collect(Collectors.groupingByConcurrent(MovementLine::productKey));

		List<ProductMovementResponse> report = byProduct.values().parallelStream()
				.map(this::movementForProduct)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(ProductMovementResponse::soldQuantity).reversed())
				.toList();
		log.info(
				"product_movement_report_generated businessId={} from={} to={} products={}",
				business.getId(),
				start,
				end,
				report.size());
		return report;
	}

	/**
	 * Projects JPA entities into immutable lines before parallel streams touch the data.
	 */
	private List<MovementLine> movementLinesFrom(List<TransactionItem> items) {
		return items.stream()
				.map(item -> new MovementLine(
						item.getTransaction().getType(),
						item.getProduct().getId(),
						item.getProduct().getName(),
						item.getProduct().getCategory(),
						item.getProduct().getBarcodes().stream()
								.map(ProductBarcode::getBarcode)
								.toList(),
						item.getQuantity(),
						item.getUnitPrice()))
				.toList();
	}

	/**
	 * Totals quantities for one transaction type across a prepared item set.
	 */
	private int quantityFor(List<MovementLine> lines, TransactionType type) {
		return lines.parallelStream()
				.filter(line -> line.type() == type)
				.mapToInt(MovementLine::quantity)
				.sum();
	}

	/**
	 * Totals line-item values for one transaction type across a prepared item set.
	 */
	private BigDecimal valueFor(List<MovementLine> lines, TransactionType type) {
		return lines.parallelStream()
				.filter(line -> line.type() == type)
				.map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * Converts grouped transaction items into one product movement response.
	 */
	private ProductMovementResponse movementForProduct(List<MovementLine> lines) {
		if (lines.isEmpty()) {
			return null;
		}
		MovementLine product = lines.getFirst();
		return new ProductMovementResponse(
				product.productId(),
				product.productName(),
				product.category(),
				product.barcodes(),
				quantityFor(lines, TransactionType.BUY),
				quantityFor(lines, TransactionType.SELL),
				valueFor(lines, TransactionType.BUY),
				valueFor(lines, TransactionType.SELL));
	}

	private record MovementLine(
			TransactionType type,
			Long productId,
			String productName,
			ProductCategory category,
			List<String> barcodes,
			int quantity,
			BigDecimal unitPrice) {

		ProductMovementKey productKey() {
			return new ProductMovementKey(productId, productName);
		}
	}

	private record ProductMovementKey(Long productId, String productName) {
	}

	private record StockLine(ProductCategory category, BigDecimal price, int stockQuantity) {
	}
}
