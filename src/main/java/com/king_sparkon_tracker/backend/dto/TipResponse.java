package com.king_sparkon_tracker.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.king_sparkon_tracker.backend.model.SupportedCurrency;
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

	public TipResponse(
			Long id,
			Long workerId,
			double tipAmount,
			double systemFee,
			double netAmount,
			LocalDateTime created,
			LocalDateTime updated,
			TipStatus status,
			String paymentReference,
			String paymentUrl,
			String qrCodeUrl,
			SupportedCurrency currency,
			String formattedTipAmount,
			String formattedSystemFee,
			String formattedNetAmount) {
		this(
				id,
				workerId,
				BigDecimal.valueOf(tipAmount),
				BigDecimal.valueOf(systemFee),
				BigDecimal.valueOf(netAmount),
				status,
				paymentReference,
				paymentUrl,
				qrCodeUrl,
				created == null ? null : created.atOffset(ZoneOffset.UTC),
				updated == null ? null : updated.atOffset(ZoneOffset.UTC));
	}

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
