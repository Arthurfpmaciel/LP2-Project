package com.agentmanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationRequest(
        @NotNull Long apiKeyId,
        @NotNull Long agentId,
        @NotBlank String input,
        @NotBlank String output,
        @NotNull @Min(0) Integer inputTokens,
        @NotNull @Min(0) Integer outputTokens,
        @NotNull @Min(0) Long latencyMs
) {
}
