package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.model.AffiliateCommission;
import com.king_sparkon_tracker.backend.model.AffiliateWithdrawal;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipWithdrawal;
import com.king_sparkon_tracker.backend.model.TransactionWithdrawal;

@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	public void logTipPaymentLink(Tip tip, String paymentUrl) {
		log.info("tip_payment_link_created tipId={} workerId={} paymentUrl={}",
				tip.getId(),
				tip.getWorkerId(),
				paymentUrl);
	}

	public void logWithdrawalRequested(TipWithdrawal withdrawal) {
		log.info("tip_withdrawal_requested withdrawalId={} workerId={} amount={} paypalEmail={}",
				withdrawal.getId(),
				withdrawal.getWorkerId(),
				withdrawal.getAmount(),
				withdrawal.getPaypalEmail());
	}

	public void logTransactionWithdrawalRequested(TransactionWithdrawal withdrawal) {
		log.info("transaction_withdrawal_requested withdrawalId={} ownerId={} businessId={} amount={} grossAmount={} feeAmount={} paypalEmail={}",
				withdrawal.getId(),
				withdrawal.getOwnerId(),
				withdrawal.getBusinessId(),
				withdrawal.getAmount(),
				withdrawal.getGrossAmount(),
				withdrawal.getFeeAmount(),
				withdrawal.getPaypalEmail());
	}

	public void logAffiliateCommissionEarned(AffiliateCommission commission) {
		log.info("affiliate_commission_earned commissionId={} affiliateId={} businessId={} subscriptionId={} amount={} rate={}",
				commission.getId(),
				commission.getAffiliateId(),
				commission.getBusinessId(),
				commission.getSubscriptionId(),
				commission.getCommissionAmount(),
				commission.getCommissionRatePercent());
	}

	public void logAffiliateWithdrawalRequested(AffiliateWithdrawal withdrawal) {
		log.info("affiliate_withdrawal_requested withdrawalId={} affiliateId={} amount={} commissionCount={} paypalLink={}",
				withdrawal.getId(),
				withdrawal.getAffiliateId(),
				withdrawal.getAmount(),
				withdrawal.getCommissionCount(),
				withdrawal.getPaypalLink());
	}
}
