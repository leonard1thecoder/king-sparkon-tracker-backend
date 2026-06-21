package com.king_sparkon_tracker.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransactionResponse(
		@Schema(description = "Transaction id.", example = "7")
		Long id,
		@Schema(description = "Transaction timestamp.")
		LocalDateTime date,
		@Schema(description = "Transaction type.", example = "SELL")
		TransactionType type,
		@Schema(description = "Business id this transaction belongs to.", example = "3")
		Long businessId,
		@Schema(description = "Business name this transaction belongs to.", example = "Owner Retail Store")
		String businessName,
		@Schema(description = "Transaction status for dashboard tables.", example = "COMPLETED")
		String status,
		@Schema(description = "Worker user id.", example = "2")
		Long employeeId,
		@Schema(description = "Owner user id.", example = "1")
		Long ownerId,
		@Schema(description = "Product line items moved by this transaction.")
		List<TransactionItemResponse> items) {

	/**
	 * Converts a transaction entity into the API response shape.
	 */
	public static TransactionResponse from(InventoryTransaction transaction) {
		return new TransactionResponse(
				transaction.getId(),
				transaction.getDate(),
				transaction.getType(),
				transaction.getBusiness() == null ? null : transaction.getBusiness().getId(),
				transaction.getBusiness() == null ? null : transaction.getBusiness().getName(),
				"COMPLETED",
				transaction.getEmployee().getId(),
				transaction.getOwner().getId(),
				transaction.getItems().stream()
						.map(TransactionItemResponse::from)
						.toList());
	}
}
