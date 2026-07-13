package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.king_sparkon_tracker.backend.model.TransactionPaymentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateTuckShopPurchaseRequest(
		@Schema(description = "Customer email used for the payment transaction.", example = "customer@example.com")
		@Email
		@Size(max = 255)
		String paymentEmail,

		@Schema(description = "Customer contact shown on the transaction. Can be email or cellphone.", example = "+27821234567")
		@Size(max = 320)
		String paymentContact,

		@Schema(description = "Optional worker receiving the tip or assisting the tuck shop purchase.", example = "12")
		Long workerId,

		@Schema(description = "Optional worker tip amount. Kept separate from product revenue.", example = "15.00")
		@DecimalMin(value = "0.00")
		@Digits(integer = 10, fraction = 2)
		BigDecimal tipAmount,

		@Schema(description = "Optional callback URL used when creating the separate worker tip payment link.")
		@Size(max = 2048)
		String tipCallbackUrl,

		@Schema(description = "Existing product items to buy.")
		@NotEmpty
		List<@Valid TuckShopPurchaseItemRequest> items,

		@Schema(description = "Worker checkout payment type. CASH and SWIPE_MACHINE complete at the counter; WEBSITE_PAYMENT creates a King Sparkon checkout.")
		TransactionPaymentType paymentType,

		@Schema(description = "Registered customer username required when a worker sends a King Sparkon checkout.")
		@Size(max = 120)
		String customerUsername
) {
	public CreateTuckShopPurchaseRequest(
			String paymentEmail,
			String paymentContact,
			Long workerId,
			BigDecimal tipAmount,
			String tipCallbackUrl,
			List<TuckShopPurchaseItemRequest> items) {
		this(paymentEmail, paymentContact, workerId, tipAmount, tipCallbackUrl, items, null, null);
	}
}
