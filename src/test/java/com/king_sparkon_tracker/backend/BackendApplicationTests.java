package com.king_sparkon_tracker.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.ai.vectorstore.pgvector.initialize-schema=false",
		"app.ai.rag.ingestion.enabled=false"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
