package com.king_sparkon_tracker.backend.dto;

import java.util.List;

import com.king_sparkon_tracker.backend.model.TransactionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTransactionRequest(
		@Schema(description = "Stock movement direction.", example = "SELL") @NotNull TransactionType type,
		@Schema(description = "Worker who handled the transaction.", example = "2") @NotNull Long employeeId,
		@Schema(description = "Owner accountable for the transaction.", example = "1") @NotNull Long ownerId,
		@Schema(description = "Products and quantities moved by this transaction.") @NotEmpty List<@Valid TransactionItemRequest> items) {
}
