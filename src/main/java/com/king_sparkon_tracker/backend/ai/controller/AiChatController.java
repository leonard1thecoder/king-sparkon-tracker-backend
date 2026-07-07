package com.king_sparkon_tracker.backend.ai.controller;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import com.king_sparkon_tracker.backend.ai.dto.AiChatResponse;
import com.king_sparkon_tracker.backend.ai.exception.AiChatException;
import com.king_sparkon_tracker.backend.ai.service.AiChatService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private static final long STREAM_TIMEOUT_MILLIS = Duration.ofMinutes(2).toMillis();

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);

        Thread.startVirtualThread(() -> {
            try {
                aiChatService.stream(request, chunk -> sendChunk(emitter, chunk));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception exception) {
                log.warn("AI chatbot stream failed: {}", exception.getMessage());
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "KING_SPARKON_AI_ASSISTANT",
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(AiChatException.class)
    public ResponseEntity<Map<String, Object>> handleAiChatException(AiChatException exception) {
        log.warn("AI chatbot request failed: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "AI_ASSISTANT_UNAVAILABLE",
                "message", exception.getMessage(),
                "timestamp", Instant.now()
        ));
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().name("chunk").data(chunk));
        } catch (IOException exception) {
            throw new AiChatException("Unable to write AI stream chunk", exception);
        }
    }
}
