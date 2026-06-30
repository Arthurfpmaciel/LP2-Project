package com.agentmanager.service.agents;

public record AgentExecutionRequest(
        String input,
        Long userId,
        Long apiKeyId
) {

    public static AgentExecutionRequest fromInput(String input) {
        return new AgentExecutionRequest(input, null, null);
    }
}
