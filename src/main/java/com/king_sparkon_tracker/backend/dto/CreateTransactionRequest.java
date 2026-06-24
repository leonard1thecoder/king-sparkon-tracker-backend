package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTransactionRequest(
		@Schema(description = "Stock movement direction.", example = "SELL") @NotNull TransactionType type,
		@Schema(description = "Transaction-level payment type for SELL movements.", example = "CASH") TransactionPaymentType paymentType,
		@Schema(description = "Backward-compatible customer payment or returnable-reference email.", example = "customer@example.com") @Email @Size(max = 255) String paymentEmail,
		@Schema(description = "Customer website-payment contact. Can be email or international cellphone number.", example = "+27821234567") @Size(max = 320) String paymentContact,
		@Schema(description = "Worker who handled the transaction.", example = "2") @NotNull Long employeeId,
		@Schema(description = "Owner accountable for the transaction.", example = "1") @NotNull Long ownerId,
		@Schema(description = "Products and quantities moved by this transaction.") @NotEmpty List<@Valid TransactionItemRequest> items) {

	public CreateTransactionRequest(
			TransactionType type,
			TransactionPaymentType paymentType,
			String paymentEmail,
			Long employeeId,
			Long ownerId,
			List<TransactionItemRequest> items) {
		this(type, paymentType, paymentEmail, paymentEmail, employeeId, ownerId, items);
	}

	public CreateTransactionRequest(
			TransactionType type,
			Long employeeId,
			Long ownerId,
			List<TransactionItemRequest> items) {
		this(type, null, null, null, employeeId, ownerId, items);
	}
}
