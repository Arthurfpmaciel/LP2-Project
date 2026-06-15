package com.agentmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InnerLLMPromptRequest(
    @NotBlank String apiKey,
    @NotNull Long agentId,
    @NotBlank String input
) {}
