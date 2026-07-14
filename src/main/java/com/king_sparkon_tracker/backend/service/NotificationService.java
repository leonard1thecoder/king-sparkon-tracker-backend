package com.king_sparkon_tracker.backend.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.AffiliateCommission;
import com.king_sparkon_tracker.backend.model.AffiliateWithdrawal;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;
import com.king_sparkon_tracker.backend.outbox.OutboxPublisher;

@Service
public class NotificationService {

	private final OutboxPublisher outboxPublisher;

	public NotificationService(OutboxPublisher outboxPublisher) {
		this.outboxPublisher = outboxPublisher;
	}

	public void logTipPaymentLink(Tip tip, String paymentUrl) {
		outboxPublisher.notification("TIP", String.valueOf(tip.getId()), "tip_payment_link_created", attributes(
				"tipId", tip.getId(),
				"workerId", tip.getWorkerId(),
				"paymentProvider", provider(paymentUrl)));
	}

	public void logWithdrawalRequested(TipWithdrawal withdrawal) {
		outboxPublisher.notification("TIP_WITHDRAWAL", String.valueOf(withdrawal.getId()), "tip_withdrawal_requested", attributes(
				"withdrawalId", withdrawal.getId(),
				"workerId", withdrawal.getWorkerId(),
				"amount", withdrawal.getAmount()));
	}

	public void logTransactionWithdrawalRequested(TransactionWithdrawal withdrawal) {
		outboxPublisher.notification("TRANSACTION_WITHDRAWAL", String.valueOf(withdrawal.getId()), "transaction_withdrawal_requested", attributes(
				"withdrawalId", withdrawal.getId(),
				"ownerId", withdrawal.getOwnerId(),
				"businessId", withdrawal.getBusinessId(),
				"amount", withdrawal.getAmount(),
				"grossAmount", withdrawal.getGrossAmount(),
				"feeAmount", withdrawal.getFeeAmount()));
	}

	public void logAffiliateCommissionEarned(AffiliateCommission commission) {
		outboxPublisher.notification("AFFILIATE_COMMISSION", String.valueOf(commission.getId()), "affiliate_commission_earned", attributes(
				"commissionId", commission.getId(),
				"affiliateId", commission.getAffiliateId(),
				"businessId", commission.getBusinessId(),
				"subscriptionId", commission.getSubscriptionId(),
				"amount", commission.getCommissionAmount(),
				"rate", commission.getCommissionRatePercent()));
	}

	public void logAffiliateWithdrawalRequested(AffiliateWithdrawal withdrawal) {
		outboxPublisher.notification("AFFILIATE_WITHDRAWAL", String.valueOf(withdrawal.getId()), "affiliate_withdrawal_requested", attributes(
				"withdrawalId", withdrawal.getId(),
				"affiliateId", withdrawal.getAffiliateId(),
				"amount", withdrawal.getAmount(),
				"commissionCount", withdrawal.getCommissionCount()));
	}

	private Map<String, Object> attributes(Object... values) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		for (int index = 0; index < values.length; index += 2) {
			attributes.put(String.valueOf(values[index]), values[index + 1]);
		}
		return attributes;
	}

	private String provider(String url) {
		if (url == null) return "unknown";
		if (url.contains("stripe")) return "stripe";
		if (url.contains("paypal")) return "paypal";
		return "external";
	}
}
