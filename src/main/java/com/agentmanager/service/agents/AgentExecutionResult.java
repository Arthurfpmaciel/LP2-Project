package com.agentmanager.service.agents;

import com.agentmanager.dto.LlmResponse;
import com.agentmanager.model.PlanType;

public record AgentExecutionResult(
        PlanType agentLevel,
        AgentIntent intent,
        LlmResponse response
) {
}
