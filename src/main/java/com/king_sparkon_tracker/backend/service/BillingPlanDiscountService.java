package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.king_sparkon_tracker.backend.dto.BillingDiscountDtos.BillingDiscountResponse;
import com.king_sparkon_tracker.backend.dto.BillingDiscountDtos.UpsertBillingDiscountRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.BillingPlanDiscount;
import com.king_sparkon_tracker.backend.model.BusinessPlan;
import com.king_sparkon_tracker.backend.model.PrivilegeRole;
import com.king_sparkon_tracker.backend.repository.BillingPlanDiscountRepository;
import com.king_sparkon_tracker.backend.repository.TrackerUserRepository;

@Service
@Transactional
public class BillingPlanDiscountService {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

	private final BillingPlanDiscountRepository discountRepository;
	private final TrackerUserRepository userRepository;

	public BillingPlanDiscountService(
			BillingPlanDiscountRepository discountRepository,
			TrackerUserRepository userRepository) {
		this.discountRepository = discountRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<BillingDiscountResponse> listForAdmin(String actorUsername) {
		requireAdmin(actorUsername);
		OffsetDateTime now = OffsetDateTime.now();
		return discountRepository.findAllByOrderByBusinessPlanAsc().stream()
				.map(discount -> BillingDiscountResponse.from(discount, now))
				.toList();
	}

	public BillingDiscountResponse upsert(
			BusinessPlan plan,
			UpsertBillingDiscountRequest request,
			String actorUsername) {
		requireAdmin(actorUsername);
		requirePaidPlan(plan);
		if (request.startsAt() != null && request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
			throw new IllegalArgumentException("Discount end date must be after the start date");
		}

		BillingPlanDiscount discount = discountRepository.findByBusinessPlan(plan)
				.orElseGet(() -> new BillingPlanDiscount(plan));
		discount.update(
				money(request.discountPercent()),
				request.label().trim(),
				request.active(),
				request.startsAt(),
				request.endsAt(),
				actorUsername);
		BillingPlanDiscount saved = discountRepository.save(discount);
		return BillingDiscountResponse.from(saved, OffsetDateTime.now());
	}

	@Transactional(readOnly = true)
	public Optional<BillingPlanDiscount> effectiveDiscount(BusinessPlan plan) {
		if (plan == null || plan == BusinessPlan.FREE_TRIAL) return Optional.empty();
		OffsetDateTime now = OffsetDateTime.now();
		return discountRepository.findByBusinessPlan(plan).filter(discount -> discount.isEffective(now));
	}

	@Transactional(readOnly = true)
	public BigDecimal effectivePrice(BusinessPlan plan, BigDecimal originalPrice) {
		BigDecimal original = money(originalPrice);
		return effectiveDiscount(plan)
				.map(discount -> original
						.multiply(ONE_HUNDRED.subtract(discount.getDiscountPercent()))
						.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP))
				.orElse(original);
	}

	private void requirePaidPlan(BusinessPlan plan) {
		if (plan != BusinessPlan.PLUS && plan != BusinessPlan.PRO) {
			throw new IllegalArgumentException("Admin discounts can only be configured for PLUS or PRO");
		}
	}

	private void requireAdmin(String actorUsername) {
		var user = userRepository.findByUsername(actorUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));
		if (user.getPrivilege() == null || user.getPrivilege().getName() != PrivilegeRole.Admin) {
			throw new IllegalArgumentException("Only administrators can manage plan discounts");
		}
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
	}
}
