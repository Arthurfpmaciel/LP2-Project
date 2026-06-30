package com.agentmanager.service.agents.tavily;

import com.agentmanager.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class TavilySearchService {

    private final RestClient restClient;
    private final String apiKey;
    private final int maxResults;

    public TavilySearchService(
            RestClient.Builder restClientBuilder,
            @Value("${tavily.base-url:https://api.tavily.com}") String baseUrl,
            @Value("${tavily.api-key:}") String apiKey,
            @Value("${tavily.max-results:5}") int maxResults
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiKey = apiKey;
        this.maxResults = maxResults;
    }

    public List<TavilySearchResult> searchImd(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Sem chave de API do Tavily cadastrada.");
        }

        try {
            TavilyResponse response = restClient.post()
                    .uri("/search")
                    .body(new TavilyRequest(
                            apiKey,
                            "site:imd.ufrn.br " + question,
                            "basic",
                            maxResults,
                            List.of("imd.ufrn.br")
                    ))
                    .retrieve()
                    .body(TavilyResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }
            return response.results().stream()
                    .map(result -> new TavilySearchResult(result.title(), result.url(), result.content()))
                    .toList();
        } catch (RestClientResponseException exception) {
            throw new BusinessException("Erro ao consultar Tavily: HTTP "
                    + exception.getStatusCode().value() + " - " + exception.getResponseBodyAsString());
        } catch (RuntimeException exception) {
            throw new BusinessException("Erro ao consultar Tavily: " + exception.getMessage());
        }
    }

    public String formatResults(List<TavilySearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "Nenhum resultado encontrado no site do IMD.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            TavilySearchResult result = results.get(i);
            builder.append("Fonte ").append(i + 1).append(":\n")
                    .append("Titulo: ").append(nullToBlank(result.title())).append("\n")
                    .append("URL: ").append(nullToBlank(result.url())).append("\n")
                    .append("Trecho: ").append(nullToBlank(result.content())).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private record TavilyRequest(
            @JsonProperty("api_key") String apiKey,
            String query,
            @JsonProperty("search_depth") String searchDepth,
            @JsonProperty("max_results") int maxResults,
            @JsonProperty("include_domains") List<String> includeDomains
    ) {
    }

    private record TavilyResponse(
            List<TavilyResult> results
    ) {
    }

    private record TavilyResult(
            String title,
            String url,
            String content
    ) {
    }
}
