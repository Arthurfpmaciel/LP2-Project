package com.agentmanager.dto;

public record ConversationTokensByApiKeyResponse(
        Long apiKeyId,
        Long totalInputTokens,
        Long totalOutputTokens,
        Long totalTokens
) {
}
