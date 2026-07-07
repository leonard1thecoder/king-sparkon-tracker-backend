package com.king_sparkon_tracker.backend.ai.retrieval;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VectorStoreAiRetrievalContextService implements AiRetrievalContextService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreAiRetrievalContextService.class);

    private final VectorStore vectorStore;
    private final StaticAiRetrievalContextService fallbackRetrievalContextService;
    private final int topK;
    private final double similarityThreshold;

    public VectorStoreAiRetrievalContextService(
            VectorStore vectorStore,
            @Value("${app.ai.rag.top-k:5}") int topK,
            @Value("${app.ai.rag.similarity-threshold:0.55}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.fallbackRetrievalContextService = new StaticAiRetrievalContextService();
        this.topK = Math.max(1, topK);
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public List<String> retrieveContext(AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            return List.of();
        }

        List<String> context = new ArrayList<>(fallbackRetrievalContextService.retrieveContext(request));

        try {
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(request.message())
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build());

            documents.stream()
                    .map(Document::getText)
                    .filter(StringUtils::hasText)
                    .map(this::compact)
                    .forEach(context::add);
        } catch (RuntimeException exception) {
            log.warn("ai_vector_retrieval_failed reason={}", exception.getMessage());
        }

        return context.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .limit(10)
                .toList();
    }

    private String compact(String text) {
        String compacted = text.replaceAll("\\s+", " ").trim();
        if (compacted.length() <= 700) {
            return compacted;
        }
        return compacted.substring(0, 700) + "...";
    }
}
