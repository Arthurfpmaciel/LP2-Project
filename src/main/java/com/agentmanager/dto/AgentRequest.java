package com.agentmanager.dto;

import com.agentmanager.model.PlanType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AgentRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull PlanType level
) {
}
