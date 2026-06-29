package com.agentmanager.service;

import com.agentmanager.dto.LlmResponse;

public interface LlmService {
    LlmResponse complete(String systemPrompt, String userPrompt);

    Integer maxTokensPerRequest();
}