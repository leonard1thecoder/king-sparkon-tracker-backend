package com.king_sparkon_tracker.backend.ai.retrieval;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.List;

/**
 * Retrieval hook for RAG context. Current implementation is static and safe;
 * later it can be replaced with pgvector or another VectorStore-backed service.
 */
public interface AiRetrievalContextService {

    List<String> retrieveContext(AiChatRequest request);
}
