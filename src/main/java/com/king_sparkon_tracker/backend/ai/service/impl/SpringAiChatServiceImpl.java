package com.king_sparkon_tracker.backend.ai.service.impl;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import com.king_sparkon_tracker.backend.ai.dto.AiChatResponse;
import com.king_sparkon_tracker.backend.ai.exception.AiChatException;
import com.king_sparkon_tracker.backend.ai.retrieval.AiRetrievalContextService;
import com.king_sparkon_tracker.backend.ai.service.AiChatService;
import com.king_sparkon_tracker.backend.ai.support.KingSparkonPromptFactory;
import com.king_sparkon_tracker.backend.ai.tool.AiReadOnlyToolContextService;
import com.king_sparkon_tracker.backend.ai.tool.DashboardReadOnlyAiTool;
import com.king_sparkon_tracker.backend.ai.tool.ProductReadOnlyAiTool;
import com.king_sparkon_tracker.backend.ai.tool.TicketReadOnlyAiTool;
import com.king_sparkon_tracker.backend.ai.tool.TipsReadOnlyAiTool;
import java.time.Instant;
import java.util.List;
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
    private final String modelName;

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
            @Value("${spring.ai.ollama.chat.model:qwen3:4b}") String modelName) {
        this.chatClient = chatClient;
        this.questionAnswerAdvisor = questionAnswerAdvisor;
        this.promptFactory = promptFactory;
        this.retrievalContextService = retrievalContextService;
        this.readOnlyToolContextService = readOnlyToolContextService;
        this.ticketReadOnlyAiTool = ticketReadOnlyAiTool;
        this.productReadOnlyAiTool = productReadOnlyAiTool;
        this.tipsReadOnlyAiTool = tipsReadOnlyAiTool;
        this.dashboardReadOnlyAiTool = dashboardReadOnlyAiTool;
        this.modelName = modelName;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        validate(request);

        String conversationId = conversationIdFor(request);
        AiChatRequest normalizedRequest = normalizeRequest(request, conversationId);

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
                    "SPRING_AI_OLLAMA",
                    buildSuggestions(normalizedRequest.message()),
                    Instant.now()
            );
        } catch (AiChatException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("AI chat request failed. model={} conversationId={}", modelName, conversationId, exception);
            throw new AiChatException("AI assistant is temporarily unavailable", exception);
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
            log.error("AI chat stream failed. model={} conversationId={}", modelName, conversationId, exception);
            throw new AiChatException("AI assistant stream is temporarily unavailable", exception);
        }
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
