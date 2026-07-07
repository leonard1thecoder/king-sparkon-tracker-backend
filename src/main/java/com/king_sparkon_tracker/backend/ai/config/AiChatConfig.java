package com.king_sparkon_tracker.backend.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {

    @Bean
    ChatClient kingSparkonChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultSystem("""
                        You are King Sparkon Assistant.
                        Help users understand King Sparkon Tracker features.
                        Never invent private business, ticket, payment, user, or worker data.
                        """)
                .build();
    }
}
