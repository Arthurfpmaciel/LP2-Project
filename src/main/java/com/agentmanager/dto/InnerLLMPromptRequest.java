package com.agentmanager.dto;

import com.agentmanager.model.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InnerLLMPromptRequest(
    @NotBlank String apiKey,
    @NotNull PlanType agentLevel,
    @NotBlank String input
) {}
