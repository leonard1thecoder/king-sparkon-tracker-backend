package com.king_sparkon_tracker.backend.ai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AiDocumentIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentIngestionRunner.class);

    private final AiDocumentIngestionService ingestionService;
    private final boolean ingestionEnabled;

    public AiDocumentIngestionRunner(
            AiDocumentIngestionService ingestionService,
            @Value("${app.ai.rag.ingestion.enabled:true}") boolean ingestionEnabled) {
        this.ingestionService = ingestionService;
        this.ingestionEnabled = ingestionEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ingestionEnabled) {
            log.info("ai_rag_ingestion_disabled");
            return;
        }

        try {
            ingestionService.ingestDefaultKnowledgeBase();
        } catch (RuntimeException exception) {
            log.warn("ai_rag_ingestion_failed reason={}", exception.getMessage());
        }
    }
}
