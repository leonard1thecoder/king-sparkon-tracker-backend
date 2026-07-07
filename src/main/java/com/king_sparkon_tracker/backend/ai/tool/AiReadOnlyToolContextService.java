package com.king_sparkon_tracker.backend.ai.tool;

import com.king_sparkon_tracker.backend.ai.dto.AiChatRequest;
import java.util.List;

/**
 * Read-only tool context hook. Keep this read-only until strict auth and confirmation flows exist.
 */
public interface AiReadOnlyToolContextService {

    List<String> availableToolContext(AiChatRequest request);
}
