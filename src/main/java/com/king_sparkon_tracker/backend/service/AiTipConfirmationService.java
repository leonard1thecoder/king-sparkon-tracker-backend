package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.AiTipConfirmationResponse;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TipRepository;

@Service
@Transactional(readOnly = true)
public class AiTipConfirmationService {

	private static final Logger log = LoggerFactory.getLogger(AiTipConfirmationService.class);
	private static final int MONEY_SCALE = 2;

	private final TipRepository tipRepository;
	private final TrackerUserService trackerUserService;
	private final ChatClient chatClient;
	private final BigDecimal withdrawalFeePercent;

	public AiTipConfirmationService(
			TipRepository tipRepository,
			TrackerUserService trackerUserService,
			ChatClient chatClient,
			@Value("${app.tips.withdrawal-fee-percent:8.5}") BigDecimal withdrawalFeePercent) {
		this.tipRepository = tipRepository;
		this.trackerUserService = trackerUserService;
		this.chatClient = chatClient;
		this.withdrawalFeePercent = withdrawalFeePercent == null ? BigDecimal.ZERO : withdrawalFeePercent;
	}

	public AiTipConfirmationResponse confirmTipForOwner(Long tipId, String ownerUsername) {
		TrackerUser owner = requireOwner(ownerUsername);
		Tip tip = tipRepository.findById(tipId)
				.orElseThrow(() -> new ResourceNotFoundException("Tip not found: " + tipId));
		requireSameBusiness(owner, tip.getWorker());
		return withAi(singleTipResponse(tip));
	}

	public AiTipConfirmationResponse confirmWorkerTipsForOwner(Long workerId, String ownerUsername) {
		TrackerUser owner = requireOwner(ownerUsername);
		TrackerUser worker = trackerUserService.getUserById(workerId, ownerUsername);
		requireWorker(worker);
		requireSameBusiness(owner, worker);
		return withAi(workerTipsResponse(worker, tipRepository.findByWorker_IdOrderByCreatedDesc(worker.getId())));
	}

	public AiTipConfirmationResponse confirmMyWorkerTips(String workerUsername) {
		TrackerUser worker = trackerUserService.getUserByUsername(workerUsername);
		requireWorker(worker);
		return withAi(workerTipsResponse(worker, tipRepository.findByWorker_IdOrderByCreatedDesc(worker.getId())));
	}

	private AiTipConfirmationResponse singleTipResponse(Tip tip) {
		BigDecimal grossAmount = normalizeMoney(tip.getTipAmount());
		BigDecimal fee = withdrawalFee(grossAmount);
		BigDecimal netAmount = grossAmount.subtract(fee).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		boolean withdrawalAssigned = tip.getWithdrawalId() != null;
		String conclusion = conclusionFor(tip.getStatus(), withdrawalAssigned);

		return new AiTipConfirmationResponse(
				tip.getId(),
				tip.getWorkerId(),
				1,
				tip.getStatus() == TipStatus.PAID ? 1 : 0,
				tip.getStatus() == TipStatus.UNPAID ? 1 : 0,
				tip.getStatus(),
				grossAmount,
				fee,
				netAmount,
				tip.getWithdrawalId(),
				withdrawalAssigned,
				tip.getCreated(),
				tip.getUpdated(),
				conclusion,
				null
		);
	}

	private AiTipConfirmationResponse workerTipsResponse(TrackerUser worker, List<Tip> tips) {
		BigDecimal grossAmount = tips.stream()
				.map(Tip::getTipAmount)
				.map(this::normalizeMoney)
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add);
		BigDecimal fee = withdrawalFee(grossAmount);
		BigDecimal netAmount = grossAmount.subtract(fee).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		long paidCount = tips.stream().filter(tip -> tip.getStatus() == TipStatus.PAID).count();
		long unpaidCount = tips.stream().filter(tip -> tip.getStatus() == TipStatus.UNPAID).count();
		boolean withdrawalAssigned = tips.stream().anyMatch(tip -> tip.getWithdrawalId() != null);
		String conclusion = paidCount == 0
				? "No paid tips are confirmed yet for this worker."
				: "Paid tips are confirmed for this worker. Check withdrawal eligibility before payout.";

		return new AiTipConfirmationResponse(
				null,
				worker.getId(),
				tips.size(),
				paidCount,
				unpaidCount,
				null,
				grossAmount,
				fee,
				netAmount,
				null,
				withdrawalAssigned,
				null,
				null,
				conclusion,
				null
		);
	}

	private AiTipConfirmationResponse withAi(AiTipConfirmationResponse response) {
		String explanation = explain(response);
		return new AiTipConfirmationResponse(
				response.tipId(),
				response.workerId(),
				response.tipCount(),
				response.paidCount(),
				response.unpaidCount(),
				response.status(),
				response.grossAmount(),
				response.estimatedWithdrawalFee(),
				response.estimatedNetAmount(),
				response.withdrawalId(),
				response.withdrawalAssigned(),
				response.created(),
				response.updated(),
				response.conclusion(),
				explanation
		);
	}

	private String explain(AiTipConfirmationResponse response) {
		try {
			return chatClient.prompt()
					.system("""
						You are King Sparkon Tips AI.
						Explain tip confirmation results in one or two short sentences.
						Never mark a tip paid, request a withdrawal, change a worker, or invent payment data.
						Be clear whether the tip is PAID, UNPAID, or only estimated for withdrawal.
						""")
					.user("""
						Tip confirmation:
						tipId=%s
						workerId=%s
						tipCount=%s
						paidCount=%s
						unpaidCount=%s
						status=%s
						grossAmount=%s
						estimatedWithdrawalFee=%s
						estimatedNetAmount=%s
						withdrawalAssigned=%s
						conclusion=%s
						""".formatted(
							response.tipId(),
							response.workerId(),
							response.tipCount(),
							response.paidCount(),
							response.unpaidCount(),
							response.status(),
							response.grossAmount(),
							response.estimatedWithdrawalFee(),
							response.estimatedNetAmount(),
							response.withdrawalAssigned(),
							response.conclusion()))
					.call()
					.content();
		} catch (RuntimeException exception) {
			log.warn("ai_tip_confirmation_failed_non_blocking reason={}", exception.getMessage());
			return response.conclusion();
		}
	}

	private TrackerUser requireOwner(String username) {
		TrackerUser owner = trackerUserService.getUserByUsername(username);
		if (owner.getPrivilege() == null || owner.getPrivilege().getName() != PrivilegeRole.Owner) {
			throw new IllegalArgumentException("Only business owners can confirm tips with AI");
		}
		return owner;
	}

	private void requireWorker(TrackerUser worker) {
		if (worker.getPrivilege() == null || worker.getPrivilege().getName() != PrivilegeRole.Worker) {
			throw new IllegalArgumentException("AI tip confirmation only supports worker tips");
		}
	}

	private void requireSameBusiness(TrackerUser owner, TrackerUser worker) {
		Business ownerBusiness = owner.getBusiness();
		Business workerBusiness = worker == null ? null : worker.getBusiness();
		if (ownerBusiness == null || workerBusiness == null || !Objects.equals(ownerBusiness.getId(), workerBusiness.getId())) {
			throw new IllegalArgumentException("Tip worker must belong to the authenticated owner business");
		}
	}

	private String conclusionFor(TipStatus status, boolean withdrawalAssigned) {
		if (status == TipStatus.PAID && withdrawalAssigned) {
			return "Tip is paid and already assigned to a withdrawal.";
		}
		if (status == TipStatus.PAID) {
			return "Tip is paid and may count toward withdrawal eligibility after the holding period.";
		}
		return "Tip is still unpaid. Do not treat it as available for withdrawal.";
	}

	private BigDecimal withdrawalFee(BigDecimal amount) {
		return normalizeMoney(amount)
				.multiply(withdrawalFeePercent)
				.divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null
				? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}
}
