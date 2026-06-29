package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.model.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransactionResponse(
		@Schema(description = "Transaction id.")
		Long id,
		@Schema(description = "Transaction timestamp.")
		LocalDateTime date,
		@Schema(description = "Transaction type.")
		TransactionType type,
		@Schema(description = "Transaction-level payment type.")
		TransactionPaymentType paymentType,
		@Schema(description = "Transaction payment status.")
		TransactionPaymentStatus paymentStatus,
		@Schema(description = "Payment reference.")
		String paymentReference,
		@Schema(description = "Hosted website payment URL.")
		String paymentUrl,
		@Schema(description = "QR code URL linked to the hosted website payment URL.")
		String paymentQrCodeUrl,
		@Schema(description = "Transaction withdrawal id.")
		Long transactionWithdrawalId,
		@Schema(description = "Customer payment or returnable-reference email.")
		String paymentEmail,
		@Schema(description = "Customer website-payment contact.")
		String paymentContact,
		@Schema(description = "Transaction total amount.")
		BigDecimal totalAmount,
		@Schema(description = "Business id this transaction belongs to.")
		Long businessId,
		@Schema(description = "Business name this transaction belongs to.")
		String businessName,
		@Schema(description = "Transaction status for dashboard tables.")
		String status,
		@Schema(description = "Worker user id.")
		Long employeeId,
		@Schema(description = "Owner user id.")
		Long ownerId,
		@Schema(description = "Product line items moved by this transaction.")
		List<TransactionItemResponse> items) {

	public static TransactionResponse from(InventoryTransaction transaction) {
		return new TransactionResponse(
				transaction.getId(),
				transaction.getDate(),
				transaction.getType(),
				transaction.getPaymentType(),
				transaction.getPaymentStatus(),
				transaction.getPaymentReference(),
				transaction.getPaymentUrl(),
				transaction.getPaymentQrCodeUrl(),
				transaction.getTransactionWithdrawalId(),
				transaction.getPaymentEmail(),
				transaction.getPaymentContact(),
				transaction.getTotalAmount(),
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
