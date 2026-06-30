package com.agentmanager.service.agents.rag;

public record KnowledgeChunk(
        String title,
        String content,
        int score
) {
}
