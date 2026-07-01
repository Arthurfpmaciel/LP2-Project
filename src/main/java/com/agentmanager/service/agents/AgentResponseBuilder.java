package com.agentmanager.service.agents;

import com.agentmanager.dto.LlmResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentResponseBuilder {

    private final String model;

    public AgentResponseBuilder(
            @Value("${llm.provider}") String provider,
            @Value("${groq.model}") String groqModel,
            @Value("${minimax.model}") String minimaxModel
    ) {
        this.model = "minimax".equalsIgnoreCase(provider) ? minimaxModel : groqModel;
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
