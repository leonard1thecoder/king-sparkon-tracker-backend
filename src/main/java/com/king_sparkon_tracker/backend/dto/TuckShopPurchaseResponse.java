package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.TransactionPaymentStatus;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;

import io.swagger.v3.oas.annotations.media.Schema;

public record TuckShopPurchaseResponse(
		@Schema(description = "Existing inventory transaction id backing this tuck shop purchase.", example = "77")
		Long transactionId,
		@Schema(description = "Business id that owns the sold products.", example = "3")
		Long businessId,
		@Schema(description = "Business name.", example = "Campus Tuck Shop")
		String businessName,
		@Schema(description = "Worker who assisted the transaction, or owner for self-service checkout.", example = "12")
		Long workerId,
		@Schema(description = "Owner accountable for the product sale.", example = "5")
		Long ownerId,
		@Schema(description = "Product revenue total only. Tips are separate.", example = "62.50")
		BigDecimal productTotal,
		@Schema(description = "Website payment status for the product transaction.", example = "PENDING")
		TransactionPaymentStatus paymentStatus,
		@Schema(description = "Payment type.", example = "WEBSITE_PAYMENT")
		TransactionPaymentType paymentType,
		@Schema(description = "Stripe payment reference for the product transaction.")
		String paymentReference,
		@Schema(description = "Stripe payment URL for the product transaction.")
		String paymentUrl,
		@Schema(description = "QR code URL for the product transaction payment URL.")
		String paymentQrCodeUrl,
		@Schema(description = "Stripe client secret for an embedded card confirmation. Never log or place this value in a URL.")
		String clientSecret,
		@Schema(description = "Optional separate worker tip response. Product revenue and tips are not mixed.")
		TipResponse tip,
		@Schema(description = "Transaction date.")
		LocalDateTime createdAt,
		@Schema(description = "Purchased items.")
		List<TuckShopPurchaseItemResponse> items
) {
	public static TuckShopPurchaseResponse from(InventoryTransaction transaction, TipResponse tip) {
		return from(transaction, tip, null);
	}

	public static TuckShopPurchaseResponse from(InventoryTransaction transaction, TipResponse tip, String clientSecret) {
		return new TuckShopPurchaseResponse(
				transaction.getId(),
				transaction.getBusiness() == null ? null : transaction.getBusiness().getId(),
				transaction.getBusiness() == null ? null : transaction.getBusiness().getName(),
				transaction.getEmployee() == null ? null : transaction.getEmployee().getId(),
				transaction.getOwner() == null ? null : transaction.getOwner().getId(),
				transaction.getTotalAmount(),
				transaction.getPaymentStatus(),
				transaction.getPaymentType(),
				transaction.getPaymentReference(),
				transaction.getPaymentUrl(),
				transaction.getPaymentQrCodeUrl(),
				clientSecret,
				tip,
				transaction.getDate(),
				transaction.getItems().stream().map(TuckShopPurchaseItemResponse::from).toList()
		);
	}
}
