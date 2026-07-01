package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateTuckShopPurchaseRequest(
		@Schema(description = "Customer email used for the Stripe website-payment transaction.", example = "customer@example.com")
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
		List<@Valid TuckShopPurchaseItemRequest> items
) {
}
