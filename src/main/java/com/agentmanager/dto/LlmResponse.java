package com.agentmanager.dto;

public record LlmResponse(
        String content,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long latencyMs,
        String model
) {
}
