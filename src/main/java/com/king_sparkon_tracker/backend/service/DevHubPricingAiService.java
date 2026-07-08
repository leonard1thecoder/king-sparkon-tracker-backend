package com.king_sparkon_tracker.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.dto.DevHubDevelopmentRequestCreateRequest;

@Service
public class DevHubPricingAiService {

	private static final Logger log = LoggerFactory.getLogger(DevHubPricingAiService.class);
	private static final Pattern STANDALONE_AI = Pattern.compile("(^|[^a-z0-9])ai([^a-z0-9]|$)");

	private final ChatClient chatClient;

	public DevHubPricingAiService(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	public DevHubQuote quote(DevHubDevelopmentRequestCreateRequest request) {
		BigDecimal minPrice = estimateMinPrice(request);
		BigDecimal maxPrice = minPrice.multiply(new BigDecimal("1.85")).setScale(2, RoundingMode.HALF_UP);
		String fallbackPlan = fallbackPlan(request, minPrice, maxPrice);
		String fallbackResponse = fallbackResponse(request, minPrice, maxPrice, fallbackPlan);

		try {
			String aiText = chatClient.prompt()
					.system("""
						You are Full King Sparkon AI for Dev Hub software-development requests.
						Return a concise automated response and a practical development plan.
						Use the supplied ZAR estimate range only; do not invent payment status or guarantees.
						Include discovery, design, backend, frontend, testing, deployment, and handover when relevant.
						""")
					.user("""
						Client: %s
						Company: %s
						Project type: %s
						Title: %s
						Description: %s
						Budget: %s
						Timeline: %s
						Estimated price range: ZAR %s - %s
						""".formatted(
							request.clientName(),
							nullToEmpty(request.companyName()),
							request.projectType(),
							request.title(),
							request.description(),
							nullToEmpty(request.budgetRange()),
							nullToEmpty(request.timeline()),
							minPrice,
							maxPrice))
					.call()
					.content();
			return new DevHubQuote(minPrice, maxPrice, fallbackPlan, aiText == null || aiText.isBlank() ? fallbackResponse : aiText.trim());
		} catch (RuntimeException exception) {
			log.warn("dev_hub_ai_quote_failed_non_blocking reason={}", exception.getMessage());
			return new DevHubQuote(minPrice, maxPrice, fallbackPlan, fallbackResponse);
		}
	}

	private BigDecimal estimateMinPrice(DevHubDevelopmentRequestCreateRequest request) {
		String text = (request.projectType() + " " + request.title() + " " + request.description()).toLowerCase(Locale.ROOT);
		BigDecimal base = new BigDecimal("8500");
		if (containsAny(text, "mobile", "android", "ios")) base = base.add(new BigDecimal("9000"));
		if (containsAny(text, "payment", "stripe", "payfast", "paypal", "checkout")) base = base.add(new BigDecimal("6500"));
		if (hasStandaloneAi(text) || containsAny(text, "artificial intelligence", "chatbot", "rag", "automation", "machine learning")) base = base.add(new BigDecimal("8500"));
		if (containsAny(text, "dashboard", "admin", "analytics", "report")) base = base.add(new BigDecimal("5000"));
		if (containsAny(text, "barcode", "qr", "scanner", "inventory")) base = base.add(new BigDecimal("6500"));
		if (containsAny(text, "backend", "api", "spring", "database")) base = base.add(new BigDecimal("6000"));
		if (containsAny(text, "frontend", "next", "react", "ui", "website")) base = base.add(new BigDecimal("4500"));
		if (request.description() != null && request.description().length() > 1500) base = base.add(new BigDecimal("5000"));
		return base.setScale(2, RoundingMode.HALF_UP);
	}

	private boolean containsAny(String text, String... words) {
		for (String word : words) {
			if (text.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasStandaloneAi(String text) {
		return STANDALONE_AI.matcher(text).find();
	}

	private String fallbackPlan(DevHubDevelopmentRequestCreateRequest request, BigDecimal minPrice, BigDecimal maxPrice) {
		return "Discovery and scope lock; UX/UI wireframes; backend/API design; frontend implementation; integration and QA; deployment; handover. Estimated investment: ZAR "
				+ minPrice + " - " + maxPrice + ".";
	}

	private String fallbackResponse(DevHubDevelopmentRequestCreateRequest request, BigDecimal minPrice, BigDecimal maxPrice, String plan) {
		return "Hi " + request.clientName() + ", thanks for sending your Dev Hub request for " + request.title()
				+ ". Based on the scope, the estimated development price is ZAR " + minPrice + " - " + maxPrice
				+ ". Proposed plan: " + plan + " You can accept the plan to start scoping or reject it if the budget/timeline does not work.";
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public record DevHubQuote(BigDecimal minPrice, BigDecimal maxPrice, String developmentPlan, String automatedResponse) {
	}
}
