package com.king_sparkon_tracker.backend.ai.config;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiRagAdvisorConfig {

    @Bean
    QuestionAnswerAdvisor kingSparkonQuestionAnswerAdvisor(
            VectorStore vectorStore,
            @Value("${app.ai.rag.top-k:5}") int topK,
            @Value("${app.ai.rag.similarity-threshold:0.55}") double similarityThreshold) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(Math.max(1, topK))
                        .similarityThreshold(similarityThreshold)
                        .build())
                .build();
    }
}
