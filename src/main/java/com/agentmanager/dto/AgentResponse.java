package com.agentmanager.dto;

import java.time.LocalDateTime;

import com.agentmanager.model.PlanType;

public record AgentResponse(
        Long id,
        String name,
        String description,
        PlanType level,
        LocalDateTime createdAt
) {
}
