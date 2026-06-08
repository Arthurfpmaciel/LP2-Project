package com.agentmanager.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long apiKeyId,
        Long agentId,
        String input,
        String output,
        Integer inputTokens,
        Integer outputTokens,
        Long latencyMs,
        LocalDateTime createdAt
) {
}
