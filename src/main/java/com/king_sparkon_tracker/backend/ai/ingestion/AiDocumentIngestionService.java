package com.king_sparkon_tracker.backend.ai.ingestion;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiDocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final KingSparkonDefaultKnowledgeBase defaultKnowledgeBase;

    public AiDocumentIngestionService(
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            KingSparkonDefaultKnowledgeBase defaultKnowledgeBase) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.defaultKnowledgeBase = defaultKnowledgeBase;
    }

    @Transactional
    public int ingestDefaultKnowledgeBase() {
        if (alreadyIngested(KingSparkonDefaultKnowledgeBase.INGESTION_KEY)) {
            log.info("ai_rag_ingestion_skipped key={}", KingSparkonDefaultKnowledgeBase.INGESTION_KEY);
            return 0;
        }

        List<Document> documents = defaultKnowledgeBase.documents().stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);
        jdbcTemplate.update(
                "INSERT INTO ai_document_ingestion_log (ingestion_key, description, document_count) VALUES (?, ?, ?)",
                KingSparkonDefaultKnowledgeBase.INGESTION_KEY,
                "Default King Sparkon Tracker RAG knowledge base",
                documents.size()
        );
        log.info("ai_rag_ingestion_completed key={} documents={}", KingSparkonDefaultKnowledgeBase.INGESTION_KEY, documents.size());
        return documents.size();
    }

    private boolean alreadyIngested(String ingestionKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_document_ingestion_log WHERE ingestion_key = ?",
                Integer.class,
                ingestionKey
        );
        return count != null && count > 0;
    }

    private Document toDocument(AiKnowledgeDocument knowledgeDocument) {
        String content = "Title: " + knowledgeDocument.title() + "\n\n" + knowledgeDocument.content();
        return new Document(content, knowledgeDocument.metadata());
    }
}
