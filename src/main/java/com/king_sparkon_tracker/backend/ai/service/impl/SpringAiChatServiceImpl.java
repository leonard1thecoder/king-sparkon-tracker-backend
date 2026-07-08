package com.king_sparkon_tracker.backend.ai.service.impl;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import com.king_sparkon_tracker.backend.ai.dto.AiChatResponse;
import com.king_sparkon_tracker.backend.ai.exception.AiChatException;
import com.king_sparkon_tracker.backend.ai.retrieval.AiRetrievalContextService;
import com.king_sparkon_tracker.backend.ai.service.AiChatService;
import com.king_sparkon_tracker.backend.ai.service.TraditionalJpaAiFallbackService;
import com.king_sparkon_tracker.backend.ai.support.KingSparkonPromptFactory;
import com.king_sparkon_tracker.backend.ai.tool.AiReadOnlyToolContextService;
import com.king_sparkon_tracker.backend.ai.tool.DashboardReadOnlyAiTool;
import com.king_sparkon_tracker.backend.ai.tool.ProductReadOnlyAiTool;
import com.king_sparkon_tracker.backend.ai.tool.TicketReadOnlyAiTool;
import com.king_sparkon_tracker.backend.ai.tool.TipsReadOnlyAiTool;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SpringAiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatServiceImpl.class);

    private final ChatClient chatClient;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;
    private final KingSparkonPromptFactory promptFactory;
    private final AiRetrievalContextService retrievalContextService;
    private final AiReadOnlyToolContextService readOnlyToolContextService;
    private final TicketReadOnlyAiTool ticketReadOnlyAiTool;
    private final ProductReadOnlyAiTool productReadOnlyAiTool;
    private final TipsReadOnlyAiTool tipsReadOnlyAiTool;
    private final DashboardReadOnlyAiTool dashboardReadOnlyAiTool;
    private final TraditionalJpaAiFallbackService traditionalJpaAiFallbackService;
    private final String providerName;
    private final String modelName;
    private final boolean aiChatEnabled;
    private final boolean fallbackEnabled;

    public SpringAiChatServiceImpl(
            ChatClient chatClient,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            KingSparkonPromptFactory promptFactory,
            AiRetrievalContextService retrievalContextService,
            AiReadOnlyToolContextService readOnlyToolContextService,
            TicketReadOnlyAiTool ticketReadOnlyAiTool,
            ProductReadOnlyAiTool productReadOnlyAiTool,
            TipsReadOnlyAiTool tipsReadOnlyAiTool,
            DashboardReadOnlyAiTool dashboardReadOnlyAiTool,
            TraditionalJpaAiFallbackService traditionalJpaAiFallbackService,
            @Value("${app.ai.chat.provider:ollama}") String providerName,
            @Value("${app.ai.chat.model:qwen3:4b}") String modelName,
            @Value("${app.ai.chat.enabled:${AI_CHAT_ENABLED:true}}") boolean aiChatEnabled,
            @Value("${app.ai.chat.fallback.enabled:${AI_CHAT_FALLBACK_ENABLED:true}}") boolean fallbackEnabled) {
        this.chatClient = chatClient;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.promptFactory = promptFactory;
        this.retrievalContextService = retrievalContextService;
        this.readOnlyToolContextService = readOnlyToolContextService;
        this.ticketReadOnlyAiTool = ticketReadOnlyAiTool;
        this.productReadOnlyAiTool = productReadOnlyAiTool;
        this.tipsReadOnlyAiTool = tipsReadOnlyAiTool;
        this.dashboardReadOnlyAiTool = dashboardReadOnlyAiTool;
        this.traditionalJpaAiFallbackService = traditionalJpaAiFallbackService;
        this.providerName = providerName;
        this.modelName = modelName;
        this.aiChatEnabled = aiChatEnabled;
        this.fallbackEnabled = fallbackEnabled;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        validate(request);

        String conversationId = conversationIdFor(request);
        AiChatRequest normalizedRequest = normalizeRequest(request, conversationId);

        if (!aiChatEnabled) {
            return fallback(normalizedRequest, conversationId, "AI chat disabled by environment");
        }

        try {
            String answer = chatClient
                    .prompt()
                    .system(systemPromptFor(normalizedRequest))
                    .advisors(questionAnswerAdvisor)
                    .tools(ticketReadOnlyAiTool, productReadOnlyAiTool, tipsReadOnlyAiTool, dashboardReadOnlyAiTool)
                    .toolContext(toolContextFor(normalizedRequest))
                    .user(normalizedRequest.message())
                    .call()
                    .content();

            return new AiChatResponse(
                    conversationId,
                    answer,
                    modelName,
                    "SPRING_AI_" + providerName.toUpperCase(Locale.ROOT),
                    buildSuggestions(normalizedRequest.message()),
                    Instant.now()
            );
        } catch (AiChatException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "ai_chat_provider_failed_non_blocking provider={} model={} conversationId={} reason={}",
                    providerName,
                    modelName,
                    conversationId,
                    rootMessage(exception)
            );
            return fallback(normalizedRequest, conversationId, failureReason(exception));
        }
    }

    @Override
    public void stream(AiChatRequest request, Consumer<String> chunkConsumer) {
        validate(request);

        if (chunkConsumer == null) {
            throw new AiChatException("Stream consumer is required");
        }

        String conversationId = conversationIdFor(request);
        AiChatRequest normalizedRequest = normalizeRequest(request, conversationId);

        if (!aiChatEnabled) {
            chunkConsumer.accept(fallback(normalizedRequest, conversationId, "AI chat disabled by environment").answer());
            return;
        }

        try {
            chatClient
                    .prompt()
                    .system(systemPromptFor(normalizedRequest))
                    .advisors(questionAnswerAdvisor)
                    .tools(ticketReadOnlyAiTool, productReadOnlyAiTool, tipsReadOnlyAiTool, dashboardReadOnlyAiTool)
                    .toolContext(toolContextFor(normalizedRequest))
                    .user(normalizedRequest.message())
                    .stream()
                    .content()
                    .toIterable()
                    .forEach(chunkConsumer);
        } catch (AiChatException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "ai_chat_stream_provider_failed_non_blocking provider={} model={} conversationId={} reason={}",
                    providerName,
                    modelName,
                    conversationId,
                    rootMessage(exception)
            );
            chunkConsumer.accept(fallback(normalizedRequest, conversationId, failureReason(exception)).answer());
        }
    }

    private AiChatResponse fallback(AiChatRequest request, String conversationId, String failureReason) {
        if (!fallbackEnabled) {
            throw new AiChatException("AI assistant is temporarily unavailable");
        }
        return traditionalJpaAiFallbackService.fallback(request, conversationId, failureReason);
    }

    private String failureReason(Exception exception) {
        String rootMessage = rootMessage(exception);
        String normalized = rootMessage.toLowerCase(Locale.ROOT);

        if (normalized.contains("429") || normalized.contains("quota") || normalized.contains("insufficient_quota")) {
            return "OpenAI quota exceeded / 429";
        }

        if (normalized.contains("billing") || normalized.contains("payment required")) {
            return "OpenAI billing/quota unavailable";
        }

        if (normalized.contains("rate limit") || normalized.contains("rate_limit")) {
            return "OpenAI rate limit reached";
        }

        return "AI provider temporarily unavailable";
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable root = throwable;

        while (current != null) {
            root = current;
            current = current.getCause();
        }

        String message = root == null ? null : root.getMessage();
        if (!StringUtils.hasText(message)) {
            message = throwable == null ? null : throwable.getMessage();
        }

        if (!StringUtils.hasText(message)) {
            return "unknown";
        }

        return message.replaceAll("\\s+", " ").trim();
    }

    private void validate(AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new AiChatException("Chat message is required");
        }
    }

    private String conversationIdFor(AiChatRequest request) {
        if (StringUtils.hasText(request.conversationId())) {
            return request.conversationId().trim();
        }
        return "kst-chat-" + UUID.randomUUID();
    }

    private AiChatRequest normalizeRequest(AiChatRequest request, String conversationId) {
        return new AiChatRequest(
                conversationId,
                request.message(),
                request.history(),
                request.currentPage(),
                request.userPrivilege()
        );
    }

    private String systemPromptFor(AiChatRequest request) {
        return promptFactory.buildSystemPrompt(
                request,
                retrievalContextService.retrieveContext(request),
                readOnlyToolContextService.availableToolContext(request)
        );
    }

    private Map<String, Object> toolContextFor(AiChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())
                ? "anonymous"
                : authentication.getName();

        return Map.of(
                "actor", actor,
                "conversationId", request.conversationId(),
                "userPrivilege", StringUtils.hasText(request.userPrivilege()) ? request.userPrivilege() : "guest",
                "currentPage", StringUtils.hasText(request.currentPage()) ? request.currentPage() : "unknown page"
        );
    }

    private List<String> buildSuggestions(String message) {
        String normalizedMessage = message == null ? "" : message.toLowerCase();

        if (normalizedMessage.contains("ticket") || normalizedMessage.contains("qr")) {
            return List.of("How do I scan a ticket?", "How does owner ticket verification work?");
        }

        if (normalizedMessage.contains("barcode") || normalizedMessage.contains("inventory") || normalizedMessage.contains("scan")) {
            return List.of("How do I track stock movement?", "How does barcode audit trail work?");
        }

        if (normalizedMessage.contains("owner") || normalizedMessage.contains("business") || normalizedMessage.contains("dashboard")) {
            return List.of("Show owner dashboard features", "How does role access work?");
        }

        if (normalizedMessage.contains("dev") || normalizedMessage.contains("qa") || normalizedMessage.contains("cloud")) {
            return List.of("Explain Dev Hub support", "What cloud maintenance do you support?");
        }

        return List.of("Barcode tracking", "QR tickets", "Owner dashboard", "Dev Hub support");
    }
}
