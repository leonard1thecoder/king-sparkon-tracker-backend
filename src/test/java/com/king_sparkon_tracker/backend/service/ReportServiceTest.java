package com.king_sparkon_tracker.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.king_sparkon_tracker.backend.dto.AlcoholReportResponse;
import com.king_sparkon_tracker.backend.dto.InventorySummaryResponse;
import com.king_sparkon_tracker.backend.dto.ProductMovementResponse;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.Privilege;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Product;
import com.king_sparkon_tracker.backend.model.ProductCategory;
import com.king_sparkon_tracker.backend.model.TransactionItem;
import com.king_sparkon_tracker.backend.model.TransactionType;
import com.king_sparkon_tracker.backend.repository.ProductRepository;
import com.king_sparkon_tracker.backend.repository.TransactionItemRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

	@Mock
	private TransactionItemRepository transactionItemRepository;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ReportService reportService;

	@Test
	void alcoholReportAggregatesAlcoholBoughtAndSold() {
		LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);
		List<TransactionItem> items = List.of(
				item(TransactionType.BUY, 10, "15.00"),
				item(TransactionType.SELL, 3, "20.00"),
				item(TransactionType.SELL, 2, "21.00"));
		when(transactionItemRepository.findByTransaction_DateBetweenAndProduct_Category(
				from,
				to,
				ProductCategory.Alcohol)).thenReturn(items);

		AlcoholReportResponse result = reportService.alcoholReport(from, to);

		assertThat(result.boughtQuantity()).isEqualTo(10);
		assertThat(result.soldQuantity()).isEqualTo(5);
		assertThat(result.boughtValue()).isEqualByComparingTo("150.00");
		assertThat(result.soldValue()).isEqualByComparingTo("102.00");
	}

	@Test
	void alcoholReportReturnsZeroesWhenNoAlcoholItemsExist() {
		LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);
		when(transactionItemRepository.findByTransaction_DateBetweenAndProduct_Category(
				from,
				to,
				ProductCategory.Alcohol)).thenReturn(List.of());

		AlcoholReportResponse result = reportService.alcoholReport(from, to);

		assertThat(result.boughtQuantity()).isZero();
		assertThat(result.soldQuantity()).isZero();
		assertThat(result.boughtValue()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.soldValue()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void alcoholReportRejectsInvalidDateRange() {
		LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 1, 1, 0, 0);

		assertThatThrownBy(() -> reportService.alcoholReport(from, to))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Report from date cannot be after to date");
	}

	@Test
	void inventorySummaryAggregatesStockAndProductCounts() {
		when(productRepository.findAll()).thenReturn(List.of(
				new Product("Beer", "6001", ProductCategory.Alcohol, new BigDecimal("20.00"), 10),
				new Product("Water", "6002", ProductCategory.NonAlcohol, new BigDecimal("10.00"), 5)));

		InventorySummaryResponse result = reportService.inventorySummary(5);

		assertThat(result.totalProducts()).isEqualTo(2);
		assertThat(result.alcoholProducts()).isEqualTo(1);
		assertThat(result.nonAlcoholProducts()).isEqualTo(1);
		assertThat(result.totalStockQuantity()).isEqualTo(15);
		assertThat(result.totalStockValue()).isEqualByComparingTo("250.00");
		assertThat(result.lowStockProducts()).isEqualTo(1);
	}

	@Test
	void productMovementReportGroupsByProductAndSortsBySoldQuantity() {
		LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);
		when(transactionItemRepository.findByTransaction_DateBetween(from, to)).thenReturn(List.of(
				item(TransactionType.BUY, 10, "15.00"),
				item(TransactionType.SELL, 3, "20.00"),
				item(TransactionType.SELL, 2, "21.00")));

		List<ProductMovementResponse> result = reportService.productMovementReport(from, to);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().barcodes()).containsExactly("6001");
		assertThat(result.getFirst().boughtQuantity()).isEqualTo(10);
		assertThat(result.getFirst().soldQuantity()).isEqualTo(5);
		assertThat(result.getFirst().boughtValue()).isEqualByComparingTo("150.00");
		assertThat(result.getFirst().soldValue()).isEqualByComparingTo("102.00");
	}

	private TransactionItem item(TransactionType type, int quantity, String unitPrice) {
		TrackerUser employee = new TrackerUser(
				"worker",
				"worker@example.com",
				"encoded",
				new Privilege(PrivilegeRole.Worker));
		TrackerUser owner = new TrackerUser(
				"owner",
				"owner@kingsparkon.co.za",
				"encoded",
				new Privilege(PrivilegeRole.Owner));
		Product product = new Product("Beer", "6001", ProductCategory.Alcohol, new BigDecimal(unitPrice), 100);
		InventoryTransaction transaction = new InventoryTransaction(type, employee, owner);
		TransactionItem item = new TransactionItem(product, quantity, new BigDecimal(unitPrice));
		transaction.addItem(item);
		return item;
	}
}
