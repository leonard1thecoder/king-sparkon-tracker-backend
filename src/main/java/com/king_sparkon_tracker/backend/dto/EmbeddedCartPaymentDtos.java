package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.king_sparkon_tracker.backend.tickets.model.TicketType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class EmbeddedCartPaymentDtos {

	private EmbeddedCartPaymentDtos() {
	}

	public record TicketItem(
			@NotBlank String eventId,
			@NotNull TicketType ticketType,
			@Min(1) int quantity
	) {
	}

	public record CreateRequest(
			@NotBlank @Size(max = 160) String idempotencyKey,
			@NotBlank @Size(max = 160) String buyerName,
			@NotBlank @Email @Size(max = 255) String buyerEmail,
			@Valid List<TuckShopPurchaseItemRequest> products,
			@Valid List<TicketItem> tickets
	) {
		public boolean empty() {
			return (products == null || products.isEmpty()) && (tickets == null || tickets.isEmpty());
		}
	}

	public record CreateResponse(
			String paymentIntentId,
			String clientSecret,
			BigDecimal amount,
			String currency,
			String status
	) {
	}

	public record StatusResponse(
			String paymentIntentId,
			BigDecimal amount,
			String currency,
			String paymentStatus,
			boolean fulfilled,
			List<TuckShopPurchaseResponse> productPurchases,
			List<String> ticketPaymentIds,
			String message
	) {
	}
}
