package com.agentmanager.service.agents.tavily;

public record TavilySearchResult(
        String title,
        String url,
        String content
) {
}
