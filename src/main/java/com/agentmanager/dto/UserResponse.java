package com.agentmanager.dto;

import java.time.LocalDateTime;

import com.agentmanager.model.PlanType;

public record UserResponse(
        Long id,
        String name,
        String email,
        PlanType planType,
        LocalDateTime createdAt
) {
}
