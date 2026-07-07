package com.king_sparkon_tracker.backend.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.king_sparkon_tracker.backend.dto.DevHubDevelopmentRequestCreateRequest;
import com.king_sparkon_tracker.backend.dto.DevHubDevelopmentRequestDecisionRequest;
import com.king_sparkon_tracker.backend.exception.ResourceNotFoundException;
import com.king_sparkon_tracker.backend.model.DevHubDevelopmentRequest;
import com.king_sparkon_tracker.backend.model.DevHubRequestStatus;
import com.king_sparkon_tracker.backend.repository.DevHubDevelopmentRequestRepository;
import com.king_sparkon_tracker.backend.service.DevHubPricingAiService.DevHubQuote;

@Service
@Transactional
public class DevHubDevelopmentRequestService {

	private final DevHubDevelopmentRequestRepository repository;
	private final DevHubPricingAiService pricingAiService;

	public DevHubDevelopmentRequestService(
			DevHubDevelopmentRequestRepository repository,
			DevHubPricingAiService pricingAiService) {
		this.repository = repository;
		this.pricingAiService = pricingAiService;
	}

	@CacheEvict(cacheNames = "devHubRequests", allEntries = true)
	public DevHubDevelopmentRequest create(DevHubDevelopmentRequestCreateRequest request) {
		DevHubDevelopmentRequest entity = new DevHubDevelopmentRequest(
				normalizeRequired(request.clientName()),
				normalizeRequired(request.emailAddress()).toLowerCase(),
				normalizeOptional(request.phoneNumber()),
				normalizeOptional(request.companyName()),
				normalizeRequired(request.projectType()),
				normalizeRequired(request.title()),
				normalizeRequired(request.description()),
				normalizeOptional(request.budgetRange()),
				normalizeOptional(request.timeline()));

		DevHubQuote quote = pricingAiService.quote(request);
		entity.applyAiQuote(quote.minPrice(), quote.maxPrice(), quote.developmentPlan(), quote.automatedResponse());
		return repository.save(entity);
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "devHubRequests", key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort + ':' + #status + ':' + #search")
	public Page<DevHubDevelopmentRequest> search(Pageable pageable, DevHubRequestStatus status, String search) {
		return repository.search(status, normalizeOptional(search), pageable);
	}

	@Transactional(readOnly = true)
	public DevHubDevelopmentRequest get(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dev Hub request not found: " + id));
	}

	@CacheEvict(cacheNames = "devHubRequests", allEntries = true)
	public DevHubDevelopmentRequest accept(Long id, DevHubDevelopmentRequestDecisionRequest request) {
		DevHubDevelopmentRequest entity = get(id);
		entity.accept(normalizeDecision(request.reason(), "Accepted after reviewing the AI development plan and price estimate."));
		return repository.save(entity);
	}

	@CacheEvict(cacheNames = "devHubRequests", allEntries = true)
	public DevHubDevelopmentRequest reject(Long id, DevHubDevelopmentRequestDecisionRequest request) {
		DevHubDevelopmentRequest entity = get(id);
		entity.reject(normalizeDecision(request.reason(), "Rejected after reviewing the AI development plan and price estimate."));
		return repository.save(entity);
	}

	private String normalizeRequired(String value) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException("Required Dev Hub value is missing");
		}
		return value.trim();
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String normalizeDecision(String value, String fallback) {
		String normalized = normalizeOptional(value);
		return normalized == null ? fallback : normalized;
	}
}
