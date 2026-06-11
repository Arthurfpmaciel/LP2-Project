package com.agentmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record InnerLLMPromptRequest(
    @NotBlank String input
) {}
