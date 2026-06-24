package com.king_sparkon_tracker.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.AffiliateLinkResponse;
import com.king_sparkon_tracker.backend.dto.CreateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.dto.UpdateAffiliateLinkRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.AffiliateLink;
import com.king_sparkon_tracker.backend.model.AffiliateLinkStatus;
import com.king_sparkon_tracker.backend.model.AffiliatePlacement;
import com.king_sparkon_tracker.backend.model.Business;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.TrackerUser;
import com.king_sparkon_tracker.backend.repository.AffiliateLinkRepository;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
public class AffiliateLinkService {

	private static final int FREE_TRIAL_BARCODE_AD_THRESHOLD = 3;
	private static final int PLUS_BARCODE_AD_THRESHOLD = 8;
	private static final int MAX_RANDOM_LIMIT = 10;

	private final AffiliateLinkRepository affiliateLinkRepository;
	private final TrackerUserRepository userRepository;
	private final BusinessRepository businessRepository;

	public AffiliateLinkService(
			AffiliateLinkRepository affiliateLinkRepository,
			TrackerUserRepository userRepository,
			BusinessRepository businessRepository) {
		this.affiliateLinkRepository = affiliateLinkRepository;
		this.userRepository = userRepository;
		this.businessRepository = businessRepository;
	}

	@Transactional
	public AffiliateLinkResponse create(CreateAffiliateLinkRequest request) {
		AffiliateLink link = new AffiliateLink(
				request.title().trim(),
				trimToNull(request.description()),
				request.affiliateUrl().trim(),
				trimToNull(request.imageUrl()),
				request.websiteName().trim(),
				trimToNull(request.category()),
				request.placement(),
				request.status(),
				request.priority() == null ? 1 : request.priority());

		applyDisplayPlans(link, request.displayPlans());

		return AffiliateLinkResponse.from(affiliateLinkRepository.save(link));
	}

	@Transactional
	public AffiliateLinkResponse update(Long id, UpdateAffiliateLinkRequest request) {
		AffiliateLink link = getExisting(id);

		if (StringUtils.hasText(request.title())) {
			link.setTitle(request.title().trim());
		}
		if (request.description() != null) {
			link.setDescription(trimToNull(request.description()));
		}
		if (StringUtils.hasText(request.affiliateUrl())) {
			link.setAffiliateUrl(request.affiliateUrl().trim());
		}
		if (request.imageUrl() != null) {
			link.setImageUrl(trimToNull(request.imageUrl()));
		}
		if (StringUtils.hasText(request.websiteName())) {
			link.setWebsiteName(request.websiteName().trim());
		}
		if (request.category() != null) {
			link.setCategory(trimToNull(request.category()));
		}
		if (request.placement() != null) {
			link.setPlacement(request.placement());
		}
		if (request.status() != null) {
			link.setStatus(request.status());
		}
		if (request.priority() != null) {
			link.setPriority(request.priority());
		}
		if (request.displayPlans() != null) {
			applyDisplayPlans(link, request.displayPlans());
		}

		return AffiliateLinkResponse.from(affiliateLinkRepository.save(link));
	}

	@Transactional(readOnly = true)
	public Page<AffiliateLinkResponse> list(Pageable pageable) {
		return affiliateLinkRepository.findAll(pageable).map(AffiliateLinkResponse::from);
	}

	@Transactional
	public List<AffiliateLinkResponse> randomForAuthenticated(
			AffiliatePlacement placement,
			Integer triggerCount,
			int limit,
			String username) {
		BusinessPlan plan = resolvePlanForUsername(username).orElse(BusinessPlan.FREE_TRIAL);
		return randomForPlan(placement, plan, triggerCount, limit);
	}

	@Transactional
	public List<AffiliateLinkResponse> randomForPublicClient(
			AffiliatePlacement placement,
			Integer triggerCount,
			int limit,
			Long workerId,
			BusinessPlan fallbackPlan) {
		BusinessPlan plan = resolvePlanForWorker(workerId).orElse(fallbackPlan == null ? BusinessPlan.FREE_TRIAL : fallbackPlan);
		return randomForPlan(placement, plan, triggerCount, limit);
	}

	@Transactional
	public AffiliateLinkResponse recordClick(Long id) {
		AffiliateLink link = getExisting(id);
		link.recordClick();
		return AffiliateLinkResponse.from(affiliateLinkRepository.save(link));
	}

	private List<AffiliateLinkResponse> randomForPlan(
			AffiliatePlacement placement,
			BusinessPlan plan,
			Integer triggerCount,
			int limit) {
		if (!shouldDisplay(placement, plan, triggerCount)) {
			return List.of();
		}

		List<AffiliateLink> candidates = affiliateLinkRepository
				.findByStatusAndPlacement(AffiliateLinkStatus.ACTIVE, placement)
				.stream()
				.filter(link -> link.supportsPlan(plan))
				.sorted(Comparator.comparing(AffiliateLink::getId, Comparator.nullsLast(Long::compareTo)))
				.toList();

		List<AffiliateLink> selected = selectWeighted(candidates, safeLimit(limit));
		selected.forEach(AffiliateLink::recordImpression);
		affiliateLinkRepository.saveAll(selected);

		return selected.stream().map(AffiliateLinkResponse::from).toList();
	}

	boolean shouldDisplay(AffiliatePlacement placement, BusinessPlan plan, Integer triggerCount) {
		BusinessPlan safePlan = plan == null ? BusinessPlan.FREE_TRIAL : plan;
		if (safePlan == BusinessPlan.PRO) {
			return false;
		}
		if (placement == AffiliatePlacement.WORKER_BARCODE_THRESHOLD) {
			int count = triggerCount == null ? 0 : triggerCount;
			return safePlan == BusinessPlan.FREE_TRIAL
					? count >= FREE_TRIAL_BARCODE_AD_THRESHOLD
					: count >= PLUS_BARCODE_AD_THRESHOLD;
		}
		return true;
	}

	private List<AffiliateLink> selectWeighted(List<AffiliateLink> candidates, int limit) {
		List<AffiliateLink> remaining = new ArrayList<>(candidates);
		List<AffiliateLink> selected = new ArrayList<>();

		while (!remaining.isEmpty() && selected.size() < limit) {
			int totalWeight = remaining.stream().mapToInt(AffiliateLink::getPriority).sum();
			int cursor = ThreadLocalRandom.current().nextInt(Math.max(totalWeight, 1));
			int running = 0;
			AffiliateLink chosen = remaining.get(0);

			for (AffiliateLink candidate : remaining) {
				running += candidate.getPriority();
				if (cursor < running) {
					chosen = candidate;
					break;
				}
			}

			selected.add(chosen);
			remaining.remove(chosen);
		}

		return selected;
	}

	private Optional<BusinessPlan> resolvePlanForUsername(String username) {
		return userRepository.findByUsername(username)
				.flatMap(this::planFromUser)
				.or(() -> businessRepository.findByOwner_Username(username).map(Business::getBusinessPlan));
	}

	private Optional<BusinessPlan> resolvePlanForWorker(Long workerId) {
		if (workerId == null) {
			return Optional.empty();
		}
		return userRepository.findById(workerId).flatMap(this::planFromUser);
	}

	private Optional<BusinessPlan> planFromUser(TrackerUser user) {
		if (user.getBusiness() != null) {
			return Optional.ofNullable(user.getBusiness().getBusinessPlan());
		}
		return Optional.empty();
	}

	private AffiliateLink getExisting(Long id) {
		return affiliateLinkRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Affiliate link not found: " + id));
	}

	private void applyDisplayPlans(AffiliateLink link, List<BusinessPlan> plans) {
		List<BusinessPlan> safePlans = plans == null || plans.isEmpty()
				? List.of(BusinessPlan.FREE_TRIAL, BusinessPlan.PLUS)
				: plans;

		link.setDisplayPlans(
				safePlans.contains(BusinessPlan.FREE_TRIAL),
				safePlans.contains(BusinessPlan.PLUS),
				safePlans.contains(BusinessPlan.PRO));
	}

	private int safeLimit(int limit) {
		return Math.min(Math.max(limit, 1), MAX_RANDOM_LIMIT);
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
