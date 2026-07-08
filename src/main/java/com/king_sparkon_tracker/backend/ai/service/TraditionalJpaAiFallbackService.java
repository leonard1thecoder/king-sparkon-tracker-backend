package com.king_sparkon_tracker.backend.ai.service;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import com.king_sparkon_tracker.backend.ai.dto.AiChatResponse;
import com.king_sparkon_tracker.backend.repository.BusinessRepository;
import com.king_sparkon_tracker.backend.repository.ProductRepository;
import com.king_sparkon_tracker.backend.repository.TipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Deterministic backend fallback for live AI outages/quota failures.
 *
 * <p>This service intentionally uses normal Java + JPA repositories only. It keeps the public
 * chatbot responsive when OpenAI returns 429 quota/billing errors and avoids exposing private row
 * details through the unauthenticated chatbot endpoint.</p>
 */
@Service
public class TraditionalJpaAiFallbackService {

    private static final Logger log = LoggerFactory.getLogger(TraditionalJpaAiFallbackService.class);
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final TipRepository tipRepository;

    public TraditionalJpaAiFallbackService(
            ProductRepository productRepository,
            BusinessRepository businessRepository,
            TipRepository tipRepository) {
        this.productRepository = productRepository;
        this.businessRepository = businessRepository;
        this.tipRepository = tipRepository;
    }

    @Transactional(readOnly = true)
    public AiChatResponse fallback(AiChatRequest request, String conversationId, String failureReason) {
        String message = request == null ? "" : nullToEmpty(request.message()).toLowerCase(Locale.ROOT);
        List<String> facts = databaseFactsFor(message);
        String answer = buildAnswer(message, facts, failureReason);

        return new AiChatResponse(
                conversationId,
                answer,
                "traditional-jpa-fallback",
                "JAVA_JPA_FALLBACK",
                suggestionsFor(message),
                Instant.now()
        );
    }

    private List<String> databaseFactsFor(String message) {
        List<String> facts = new ArrayList<>();

        if (message.isBlank()
                || containsAny(message, "barcode", "scan", "scanner", "inventory", "stock", "product", "dashboard", "owner", "business")) {
            addCount(facts, "Products", productRepository::count);
            addCount(facts, "Low-stock products", () -> productRepository.countByStockQuantityLessThanEqual(LOW_STOCK_THRESHOLD));
        }

        if (message.isBlank() || containsAny(message, "dashboard", "owner", "business", "company", "register")) {
            addCount(facts, "Businesses", businessRepository::count);
        }

        if (message.isBlank() || containsAny(message, "tip", "tips", "worker", "payment", "withdraw")) {
            addCount(facts, "Tips", tipRepository::count);
        }

        return List.copyOf(facts);
    }

    private void addCount(List<String> facts, String label, Supplier<Long> countSupplier) {
        try {
            Long count = countSupplier.get();
            facts.add(label + ": " + (count == null ? 0 : count));
        } catch (RuntimeException exception) {
            log.warn("ai_jpa_fallback_count_failed label={} reason={}", label, exception.getMessage());
        }
    }

    private String buildAnswer(String message, List<String> facts, String failureReason) {
        StringBuilder answer = new StringBuilder();
        answer.append("King Sparkon Assistant is running in Java/JPA fallback mode because the live AI provider is currently quota or billing limited");

        if (StringUtils.hasText(failureReason)) {
            answer.append(" (").append(failureReason).append(")");
        }

        answer.append(". Core backend features still work; only generative OpenAI wording is paused until quota/billing is restored. ");

        if (containsAny(message, "barcode", "scan", "scanner", "inventory", "stock", "product")) {
            answer.append("Barcode and inventory support should continue through the normal product, barcode, stock, and transaction JPA flows. ");
            answer.append("Use the product endpoints for product setup, barcode creation, stock updates, and scan/audit tracking. ");
        } else if (containsAny(message, "ticket", "qr", "verify", "event")) {
            answer.append("QR ticket support should continue through the ticket purchase, QR generation, and worker verification flows. ");
            answer.append("The fallback will not invent ticket state; verify live ticket status through the ticket endpoints. ");
        } else if (containsAny(message, "tip", "tips", "worker", "payment", "withdraw")) {
            answer.append("Worker tips should continue through the deterministic tips/payment/withdrawal backend flow. ");
            answer.append("Fallback mode will not mark payments paid or trigger withdrawals; owners must use the normal secured endpoints. ");
        } else if (containsAny(message, "dev", "qa", "cloud", "ci", "deploy", "maintenance")) {
            answer.append("For Dev Hub requests, use the deterministic Java quote path: discovery, UX/UI, backend, frontend, QA, deployment, and handover. ");
            answer.append("AI wording can enhance the response later, but quote creation must not depend on OpenAI quota. ");
        } else {
            answer.append("Ask about barcode tracking, QR tickets, owner dashboards, tips, Dev Hub, QA, or cloud maintenance and the fallback will answer from deterministic backend rules. ");
        }

        if (!facts.isEmpty()) {
            answer.append("Safe database snapshot: ").append(String.join("; ", facts)).append(".");
        }

        return answer.toString().trim();
    }

    private List<String> suggestionsFor(String message) {
        if (containsAny(message, "barcode", "scan", "inventory", "stock", "product")) {
            return List.of("How stock tracking works", "How barcode audit works", "Owner inventory setup", "Worker scan flow");
        }

        if (containsAny(message, "ticket", "qr", "verify", "event")) {
            return List.of("QR ticket flow", "Worker verification", "Owner ticket setup", "Customer purchase flow");
        }

        if (containsAny(message, "tip", "tips", "worker", "withdraw")) {
            return List.of("Worker tip QR", "Owner payout flow", "Tip status rules", "Withdrawal minimums");
        }

        if (containsAny(message, "dev", "qa", "cloud", "ci", "deploy")) {
            return List.of("Dev Hub quote", "QA support", "Cloud maintenance", "CI/CD support");
        }

        return List.of("Barcode tracking", "QR tickets", "Owner dashboard", "Dev Hub support");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
