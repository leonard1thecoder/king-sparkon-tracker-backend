package com.king_sparkon_tracker.backend.ai.ingestion;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KingSparkonDefaultKnowledgeBase {

    public static final String INGESTION_KEY = "king-sparkon-default-rag-v1";

    public List<AiKnowledgeDocument> documents() {
        return List.of(
                document(
                        "platform-overview",
                        "overview",
                        "King Sparkon Tracker overview",
                        "King Sparkon Tracker is a scan-first business platform for barcode inventory tracking, QR ticket verification, owner dashboards, worker operations, buyer access, affiliate marketing, Dev Hub support, QA, CI/CD, cloud maintenance, worker tips, and audit-ready reporting."
                ),
                document(
                        "barcode-workflows",
                        "barcode",
                        "Barcode inventory workflow",
                        "Barcode tracking helps workers and owners scan products, confirm product movement, monitor stock changes, and keep audit trails visible in dashboards. Product and barcode lookups require authenticated backend access when private business data is involved."
                ),
                document(
                        "ticket-workflows",
                        "ticket",
                        "QR ticket workflow",
                        "QR tickets support event creation, buyer purchase, QR verification, worker scanning, owner ticket management, and event promotion flows. The assistant may explain the workflow publicly, but private ticket lookup requires authentication and role-safe access."
                ),
                document(
                        "tips-workflows",
                        "tips",
                        "Worker tips workflow",
                        "Worker tips support worker-specific tip links, owner visibility, withdrawal controls, holding periods, and dashboard totals. The assistant must not perform payment, withdrawal, or paid-status changes without explicit authenticated backend workflows."
                ),
                document(
                        "dashboard-workflows",
                        "dashboard",
                        "Owner dashboard workflow",
                        "Owner dashboards focus on business operations, worker visibility, product activity, tickets, reports, billing, tips, transactions, audit logs, and role-safe access. Dashboard summaries require authenticated owner access."
                ),
                document(
                        "dev-hub-support",
                        "devhub",
                        "Dev Hub support",
                        "Dev Hub support covers software delivery, QA, CI/CD, cloud maintenance, deployment readiness, observability, test coverage, and production support for business platforms."
                ),
                document(
                        "ai-safety-policy",
                        "safety",
                        "AI assistant safety policy",
                        "King Sparkon Assistant is read-only by default. It may explain workflows and retrieve authorized summaries, but it must not create, update, delete, pay, verify, withdraw, or mutate private business records unless a separate authenticated workflow explicitly performs that action."
                )
        );
    }

    private AiKnowledgeDocument document(String source, String category, String title, String content) {
        return new AiKnowledgeDocument(
                source,
                category,
                title,
                content,
                Map.of(
                        "source", source,
                        "category", category,
                        "title", title,
                        "tenant", "public",
                        "version", "v1"
                )
        );
    }
}
