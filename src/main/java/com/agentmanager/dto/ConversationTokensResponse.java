package com.agentmanager.dto;

public record ConversationTokensResponse(
        Long userId,
        Long totalInputTokens,
        Long totalOutputTokens,
        Long totalTokens
) {
}
