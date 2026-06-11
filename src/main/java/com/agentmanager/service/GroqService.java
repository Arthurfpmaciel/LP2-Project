package com.agentmanager.service;

import com.agentmanager.dto.LlmMessage;
import com.agentmanager.dto.LlmResponse;
import com.agentmanager.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GroqService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;

    public GroqService(
            RestClient.Builder restClientBuilder,
            @Value("${groq.base-url}") String baseUrl,
            @Value("${groq.api-key:}") String apiKey,
            @Value("${groq.model}") String model,
            @Value("${groq.temperature}") Double temperature,
            @Value("${groq.max-tokens}") Integer maxTokens
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public LlmResponse complete(String prompt) {
        return chat(List.of(new LlmMessage("user", prompt)));
    }

    public LlmResponse complete(String systemPrompt, String userPrompt) {
        return chat(List.of(
                new LlmMessage("system", systemPrompt),
                new LlmMessage("user", userPrompt)
        ));
    }

    public LlmResponse chat(List<LlmMessage> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Sem chave de API do Groq cadastrada.");
        }
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("Mensagem não pode ser vazia");
        }

        Instant start = Instant.now();
        try {
            GroqChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(new GroqChatRequest(model, messages, temperature, maxTokens))
                    .retrieve()
                    .body(GroqChatResponse.class);

            return toLlmResponse(response, Duration.between(start, Instant.now()).toMillis());
        } catch (RestClientResponseException exception) {
            throw new BusinessException("Erro ao chamar a Groq: HTTP "
                    + exception.getStatusCode().value() + " - " + exception.getResponseBodyAsString());
        } catch (RuntimeException exception) {
            throw new BusinessException("Erro ao chamar a Groq: " + exception.getMessage());
        }
    }

    private LlmResponse toLlmResponse(GroqChatResponse response, Long latencyMs) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException("Groq retornou uma resposta vazia.");
        }

        GroqChoice firstChoice = response.choices().get(0);
        String content = firstChoice.message() == null ? "" : firstChoice.message().content();
        GroqUsage usage = response.usage();

        content = cleanThinking(content);

        Integer inputTokens = usage == null ? 0 : usage.promptTokens();
        Integer outputTokens = usage == null ? 0 : usage.completionTokens();
        Integer totalTokens = usage == null ? inputTokens + outputTokens : usage.totalTokens();

        return new LlmResponse(content, inputTokens, outputTokens, totalTokens, latencyMs, response.model());
    }

    private String cleanThinking(String content) {
    if (content == null) {
        return "";
    }

    return content.replaceAll("(?s)<think>.*?</think>\\s*", "").trim();
    }

    private record GroqChatRequest(
            String model,
            List<LlmMessage> messages,
            Double temperature,
            @JsonProperty("max_completion_tokens") Integer maxTokens
    ) {
    }

    private record GroqChatResponse(
            String model,
            List<GroqChoice> choices,
            GroqUsage usage
    ) {
    }

    private record GroqChoice(
            LlmMessage message
    ) {
    }

    private record GroqUsage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}
