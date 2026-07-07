package com.king_sparkon_tracker.backend.ai.retrieval;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight deterministic fallback used by tests and as a safety net for vector retrieval failures.
 */
public class StaticAiRetrievalContextService implements AiRetrievalContextService {

    @Override
    public List<String> retrieveContext(AiChatRequest request) {
        String message = request == null || request.message() == null
                ? ""
                : request.message().toLowerCase(Locale.ROOT);

        List<String> context = new ArrayList<>();
        context.add("King Sparkon Tracker is a scan-first business platform for barcode inventory, QR tickets, owners, workers, buyers, affiliates, QA, CI/CD, cloud maintenance, and audit-ready reporting.");

        if (message.contains("barcode") || message.contains("scan") || message.contains("inventory")) {
            context.add("Barcode support focuses on scanning products, tracking stock movement, creating audit trails, and helping workers or owners verify product activity.");
        }

        if (message.contains("ticket") || message.contains("qr") || message.contains("event")) {
            context.add("QR ticket support focuses on event creation, buyer ticket purchase, QR verification, worker scanning, owner ticket management, and event boost workflows.");
        }

        if (message.contains("owner") || message.contains("business") || message.contains("dashboard")) {
            context.add("Owner dashboard support focuses on business operations, worker visibility, reporting, billing, tickets, product workflows, and role-safe access.");
        }

        if (message.contains("tip") || message.contains("worker")) {
            context.add("Worker tips support focuses on worker tip links, owner withdrawal controls, holding periods, and dashboard visibility. Payment-sensitive operations require authenticated backend lookup.");
        }

        if (message.contains("dev") || message.contains("qa") || message.contains("cloud") || message.contains("ci")) {
            context.add("Dev Hub support focuses on software delivery, CI/CD, QA, cloud maintenance, deployment readiness, observability, and production support.");
        }

        return List.copyOf(context);
    }
}
