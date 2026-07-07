package com.king_sparkon_tracker.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.king_sparkon_tracker.backend.dto.BarcodeMatchType;
import com.king_sparkon_tracker.backend.dto.BarcodeVerificationResponse;

@Service
public class AiBarcodeExplanationService {

	private static final Logger log = LoggerFactory.getLogger(AiBarcodeExplanationService.class);

	private final ChatClient chatClient;

	public AiBarcodeExplanationService(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	public String explain(BarcodeVerificationResponse verification) {
		if (verification == null) {
			return "No barcode verification result was available.";
		}

		try {
			return chatClient.prompt()
					.system("""
						You are King Sparkon Barcode AI.
						Explain barcode verification results in one or two short sentences.
						Never invent product, business, payment, or stock data.
						If the result is a reusable product barcode, explain that it can belong to many stock units.
						If the result is a stock unit, explain the exact unit availability safely.
						""")
					.user("""
						Verification result:
						input=%s
						matchType=%s
						status=%s
						productId=%s
						productName=%s
						productBarcode=%s
						stockQuantity=%s
						unitCode=%s
						availabilityStatus=%s
						claimStatus=%s
						message=%s
						""".formatted(
							verification.normalizedValue(),
							verification.matchType(),
							verification.status(),
							verification.productId(),
							verification.productName(),
							verification.productBarcode(),
							verification.stockQuantity(),
							verification.unitCode(),
							verification.availabilityStatus(),
							verification.claimStatus(),
							verification.message()))
					.call()
					.content();
		} catch (RuntimeException exception) {
			log.warn("ai_barcode_explanation_failed_non_blocking reason={}", exception.getMessage());
			return fallbackExplanation(verification);
		}
	}

	private String fallbackExplanation(BarcodeVerificationResponse verification) {
		if (verification.matchType() == BarcodeMatchType.PRODUCT_BARCODE) {
			return "This barcode matches a reusable product barcode. It identifies the product, while unique King Sparkon unit codes identify exact physical stock units.";
		}

		if (verification.matchType() == BarcodeMatchType.STOCK_UNIT) {
			return "This scan matches a unique King Sparkon stock unit. Use the unit availability status to decide whether it can be sold or claimed.";
		}

		return "No stored product barcode or stock-unit code matched this scan. The owner can create or link the product after confirming the barcode value.";
	}
}
