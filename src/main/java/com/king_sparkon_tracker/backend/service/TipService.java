package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.AffiliateTipRequest;
import com.king_sparkon_tracker.backend.dto.TipRequest;
import com.king_sparkon_tracker.backend.dto.TipResponse;
import com.king_sparkon_tracker.backend.dto.UpdateTipStatusRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.model.Tip;
import com.king_sparkon_tracker.backend.model.TipStatus;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import com.king_sparkon_tracker.backend.security.AuthenticatedActor;
import com.king_sparkon_tracker.backend.service.StripeService.CreatedTipPaymentLink;

@Service
public class TipService {

	private static final int MONEY_SCALE = 2;

	private final TipRepository tipRepository;
	private final TrackerUserService trackerUserService;
	private final AuthenticatedActorService authenticatedActorService;
	private final StripeService stripeService;
	private final NotificationService notificationService;
	private final SubscriberService subscriberService;

	public TipService(
			TipRepository tipRepository,
			TrackerUserService trackerUserService,
			AuthenticatedActorService authenticatedActorService,
			StripeService stripeService,
			NotificationService notificationService,
			SubscriberService subscriberService) {
		this.tipRepository = tipRepository;
		this.trackerUserService = trackerUserService;
		this.authenticatedActorService = authenticatedActorService;
		this.stripeService = stripeService;
		this.notificationService = notificationService;
		this.subscriberService = subscriberService;
	}

	@Transactional
	public TipResponse createTip(TipRequest request) {
		TrackerUser worker = trackerUserService.getUserById(request.workerId());
		requireTippableUser(worker);
		TipResponse response = createTipForRecipient(worker, request.tipAmount(), request.callbackUrl());
		subscriberService.subscribeTipPaymentClient(request.clientContact());
		return response;
	}

	@Transactional
	public TipResponse createAffiliateTip(AffiliateTipRequest request, String affiliateUsername) {
		TrackerUser affiliate = trackerUserService.getUserByUsername(affiliateUsername);
		requireAffiliate(affiliate);
		return createTipForRecipient(affiliate, request.tipAmount(), request.callbackUrl());
	}

	private TipResponse createTipForRecipient(TrackerUser recipient, BigDecimal requestedTipAmount, String callbackUrl) {
		BigDecimal tipAmount = normalizeMoney(requestedTipAmount);
		if (tipAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Tip amount must be greater than zero");
		}

		Tip tip = tipRepository.save(new Tip(recipient, tipAmount));
		BigDecimal systemFee = noCreationFee();
		BigDecimal netAmount = netAmount(tipAmount);
		CreatedTipPaymentLink paymentLink = stripeService.createTipPaymentLink(
				tip,
				systemFee,
				netAmount,
				callbackUrl);

		tip.markPaymentReference(paymentLink.stripeId());
		Tip savedTip = tipRepository.save(tip);
		notificationService.logTipPaymentLink(savedTip, paymentLink.paymentUrl());

		return TipResponse.from(
				savedTip,
				systemFee,
				netAmount,
				paymentLink.paymentUrl(),
				paymentLink.qrCodeUrl());
	}

	@Transactional
	public TipResponse updateTipStatus(Long tipId, UpdateTipStatusRequest request, String actorUsername) {
		Tip tip = tipRepository.findById(tipId)
				.orElseThrow(() -> new ResourceNotFoundException("Tip not found: " + tipId));
		authenticatedActorService.requireBusinessAccess(actorUsername, businessIdFor(tip.getWorker()));

		if (request.status() != TipStatus.PAID) {
			throw new IllegalArgumentException("Only PAID status updates are supported");
		}

		tip.markPaid();
		Tip savedTip = tipRepository.save(tip);
		return response(savedTip);
	}

	@Transactional(readOnly = true)
	public List<TipResponse> getTipsForWorker(Long workerId, String actorUsername) {
		TrackerUser worker = trackerUserService.getUserById(workerId);
		authenticatedActorService.requireBusinessAccess(actorUsername, businessIdFor(worker));
		return responsesForWorker(workerId);
	}

	@Transactional(readOnly = true)
	public List<TipResponse> getTipsForCurrentWorker(String workerUsername) {
		TrackerUser worker = trackerUserService.getUserByUsername(workerUsername);
		if (worker.getPrivilege() == null || worker.getPrivilege().getName() != PrivilegeRole.Worker) {
			throw new IllegalArgumentException("Only workers can view their own tips");
		}
		requireWorkerTipsPrivilege(worker);
		return responsesForWorker(worker.getId());
	}

	@Transactional(readOnly = true)
	public List<TipResponse> getTipsForCurrentOwner(String ownerUsername) {
		AuthenticatedActor owner = authenticatedActorService.current(ownerUsername);
		if (owner.role() != PrivilegeRole.Owner) {
			throw new AccessDeniedException("Only business owners can view business tips");
		}
		return tipRepository.findByWorker_Business_IdOrderByCreatedDesc(owner.requireBusinessId())
				.stream()
				.map(this::response)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TipResponse> getTipsForAffiliate(String affiliateUsername) {
		TrackerUser affiliate = trackerUserService.getUserByUsername(affiliateUsername);
		requireAffiliate(affiliate);
		return responsesForWorker(affiliate.getId());
	}

	@Transactional(readOnly = true)
	public List<TipResponse> getTipsByStatus(TipStatus status, String actorUsername) {
		AuthenticatedActor actor = authenticatedActorService.current(actorUsername);
		if (actor.isAdmin()) {
			return tipRepository.findByStatusOrderByCreatedDesc(status)
					.stream()
					.map(this::response)
					.toList();
		}
		if (actor.role() != PrivilegeRole.Owner) {
			throw new AccessDeniedException("Only administrators and business owners can list tips by status");
		}
		return tipRepository.findByWorker_Business_IdAndStatusOrderByCreatedDesc(actor.requireBusinessId(), status)
				.stream()
				.map(this::response)
				.toList();
	}

	BigDecimal systemFee(BigDecimal tipAmount) {
		return noCreationFee();
	}

	BigDecimal netAmount(BigDecimal tipAmount) {
		return normalizeMoney(tipAmount);
	}

	private List<TipResponse> responsesForWorker(Long workerId) {
		return tipRepository.findByWorker_IdOrderByCreatedDesc(workerId)
				.stream()
				.map(this::response)
				.toList();
	}

	private Long businessIdFor(TrackerUser user) {
		return user == null || user.getBusiness() == null ? null : user.getBusiness().getId();
	}

	private TipResponse response(Tip tip) {
		return TipResponse.from(tip, systemFee(tip.getTipAmount()), netAmount(tip.getTipAmount()), null, null);
	}

	private BigDecimal noCreationFee() {
		return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizeMoney(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
				: amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private void requireTippableUser(TrackerUser user) {
		if (user.getPrivilege() == null
				|| (user.getPrivilege().getName() != PrivilegeRole.Worker
				&& user.getPrivilege().getName() != PrivilegeRole.Affiliate)) {
			throw new IllegalArgumentException("Tips can only be created for worker or affiliate accounts");
		}
		if (user.getPrivilege().getName() == PrivilegeRole.Worker) {
			requireWorkerTipsPrivilege(user);
		}
	}

	private void requireWorkerTipsPrivilege(TrackerUser worker) {
		if (!worker.isTipQrCodeEnabled()) {
			throw new IllegalArgumentException("This worker is not privileged to receive tips. The business owner must enable tips for this worker account");
		}
	}

	private void requireAffiliate(TrackerUser user) {
		if (user.getPrivilege() == null || user.getPrivilege().getName() != PrivilegeRole.Affiliate) {
			throw new IllegalArgumentException("Only affiliate accounts can manage affiliate tips");
		}
	}
}
