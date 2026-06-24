package com.king_sparkon_tracker.backend.controller;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.king_sparkon_tracker.backend.config.OpenApiConfig;
import com.king_sparkon_tracker.backend.dto.CreateTransactionRequest;
import com.king_sparkon_tracker.backend.dto.PageResponse;
import com.king_sparkon_tracker.backend.dto.TransactionResponse;
import com.king_sparkon_tracker.backend.model.InventoryTransaction;
import com.king_sparkon_tracker.backend.model.TransactionPaymentType;
import com.king_sparkon_tracker.backend.repository.InventoryTransactionRepository;
import com.king_sparkon_tracker.backend.service.SubscriberService;
import com.king_sparkon_tracker.backend.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Transactions", description = "BUY and SELL inventory movements that update product stock.")
public class TransactionController {

	private final TransactionService transactionService;
	private final SubscriberService subscriberService;
	private final InventoryTransactionRepository transactionRepository;

	public TransactionController(
			TransactionService transactionService,
			SubscriberService subscriberService,
			InventoryTransactionRepository transactionRepository) {
		this.transactionService = transactionService;
		this.subscriberService = subscriberService;
		this.transactionRepository = transactionRepository;
	}

	/**
	 * Records a stock movement, applies stock changes, captures the authenticated actor,
	 * and auto-subscribes website-payment contacts after a successful transaction.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create transaction", description = "Creates a BUY or SELL transaction with one or more product line items.")
	public TransactionResponse createTransaction(
			@Valid @RequestBody CreateTransactionRequest request,
			@Parameter(hidden = true) Principal principal) {
		InventoryTransaction transaction = transactionService.createTransaction(request, principal.getName());
		if (request.paymentType() == TransactionPaymentType.WEBSITE_PAYMENT) {
			String subscriberContact = StringUtils.hasText(request.paymentContact()) ? request.paymentContact().trim() : request.paymentEmail();
			subscriberService.subscribeWebsitePaymentClient(subscriberContact);
			if (StringUtils.hasText(subscriberContact)) {
				transaction.setPaymentContact(subscriberContact);
				transaction = transactionRepository.save(transaction);
			}
		}
		return TransactionResponse.from(transaction);
	}

	/**
	 * Lists transaction history for owner review and reconciliation.
	 */
	@GetMapping
	@Operation(summary = "List transactions", description = "Owner-only endpoint for paginated transaction history.")
	public PageResponse<TransactionResponse> listTransactions(
			@Parameter(description = "Zero-based page number.") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size from 1 to 100.") @RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) Principal principal) {
		return PageResponse.from(transactionService.listTransactions(pageable(page, size), principal.getName()), TransactionResponse::from);
	}

	/**
	 * Returns one transaction including its product line items.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Get transaction", description = "Owner-only endpoint for viewing one transaction.")
	public TransactionResponse getTransactionById(
			@Parameter(description = "Transaction id.") @PathVariable Long id,
			@Parameter(hidden = true) Principal principal) {
		return TransactionResponse.from(transactionService.getTransactionById(id, principal.getName()));
	}

	/**
	 * Caps pagination input so transaction history remains safe to query in production.
	 */
	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(safePage, safeSize);
	}
}
