package com.king_sparkon_tracker.backend.ai.service;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import com.king_sparkon_tracker.backend.ai.dto.AiChatResponse;
import java.util.function.Consumer;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);

    void stream(AiChatRequest request, Consumer<String> chunkConsumer);
}
