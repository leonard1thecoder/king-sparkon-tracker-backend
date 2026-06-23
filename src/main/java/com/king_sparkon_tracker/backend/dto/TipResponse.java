package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;

public record TipResponse(
		Long id,
		Long workerId,
		BigDecimal tipAmount,
		BigDecimal systemFee,
		BigDecimal netAmount,
		TipStatus status,
		String paymentReference,
		String paymentUrl,
		String qrCodeUrl,
		OffsetDateTime created,
		OffsetDateTime updated
) {

	public static TipResponse from(
			Tip tip,
			BigDecimal systemFee,
			BigDecimal netAmount,
			String paymentUrl,
			String qrCodeUrl) {
		return new TipResponse(
				tip.getId(),
				tip.getWorkerId(),
				tip.getTipAmount(),
				systemFee,
				netAmount,
				tip.getStatus(),
				tip.getPaymentReference(),
				paymentUrl,
				qrCodeUrl,
				tip.getCreated(),
				tip.getUpdated()
		);
	}
}
