package com.agentmanager.service.agents;

import com.agentmanager.dto.LlmResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentResponseBuilder {

    private final String model;

    public AgentResponseBuilder(@Value("${groq.model}") String model) {
        this.model = model;
    }

    public LlmResponse build(String content, AgentTokenUsage tokenUsage, Instant start) {
        int inputTokens = tokenUsage.inputTokens();
        int outputTokens = tokenUsage.outputTokens();
        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        return new LlmResponse(
                content,
                inputTokens,
                outputTokens,
                inputTokens + outputTokens,
                latencyMs,
                model
        );
    }
}
