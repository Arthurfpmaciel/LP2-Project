package com.agentmanager.service.agents;

import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;

public record AgentTokenUsage(
        int inputTokens,
        int outputTokens
) {

    public static AgentTokenUsage empty() {
        return new AgentTokenUsage(0, 0);
    }

    public static AgentTokenUsage from(Result<String> result) {
        if (result == null || result.tokenUsage() == null) {
            return empty();
        }
        TokenUsage usage = result.tokenUsage();
        return new AgentTokenUsage(
                safeInt(usage.inputTokenCount()),
                safeInt(usage.outputTokenCount())
        );
    }

    public AgentTokenUsage plus(AgentTokenUsage other) {
        return new AgentTokenUsage(
                inputTokens + other.inputTokens(),
                outputTokens + other.outputTokens()
        );
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
