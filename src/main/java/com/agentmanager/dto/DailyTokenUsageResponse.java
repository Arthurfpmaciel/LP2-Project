package com.agentmanager.dto;

import com.agentmanager.model.PlanType;

public record DailyTokenUsageResponse(
        Long userId,
        PlanType planType,
        Long consumedTokens,
        Long dailyTokenLimit,
        Double consumptionPercentage
) {
}