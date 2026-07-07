package com.king_sparkon_tracker.backend.ai.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KingSparkonDefaultKnowledgeBaseTest {

    @Test
    void documentsReturnsDefaultKnowledgeBaseForRagIngestion() {
        KingSparkonDefaultKnowledgeBase knowledgeBase = new KingSparkonDefaultKnowledgeBase();

        assertThat(knowledgeBase.documents())
                .hasSizeGreaterThanOrEqualTo(6)
                .anySatisfy(document -> assertThat(document.category()).isEqualTo("barcode"))
                .anySatisfy(document -> assertThat(document.category()).isEqualTo("ticket"))
                .anySatisfy(document -> assertThat(document.category()).isEqualTo("safety"));
    }
}
